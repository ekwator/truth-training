use chrono::Utc;
use clap::{Parser, Subcommand};
use core_lib::expert_simple::{evaluate_answers, questions_for_context};
use core_lib::{
    NewTruthEvent, NewStatement, add_impact, add_truth_event, add_statement, get_truth_event, get_statement,
    init_db, open_db, recalc_progress_metrics, seed_knowledge_base, set_event_detected,
};
use std::collections::HashMap;

/// Simple CLI over core-lib for local testing
#[derive(Parser)]
#[command(name = "truth-training")]
#[command(about = "Truth Training CLI", long_about = None)]
struct Cli {
    /// Path to sqlite database file
    #[arg(long, default_value = "truth_db.sqlite")]
    db: String,

    #[command(subcommand)]
    cmd: Commands,
}

#[derive(Subcommand)]
enum Commands {
    /// Initialize DB schema
    Init,
    /// Seed reference knowledge base (locale: ru|en)
    Seed {
        #[arg(long, default_value = "ru")]
        locale: String,
    },
    /// Add a truth event
    AddEvent {
        /// Description
        #[arg(long)]
        description: String,
        /// Context id (must exist)
        #[arg(long)]
        context: i64,
        /// Vector: true = outgoing from user, false = incoming
        #[arg(long, default_value_t = true)]
        vector: bool,
        /// Start timestamp (unix). Defaults to now.
        #[arg(long)]
        start: Option<i64>,
    },
    /// Mark event detected (true/false), optionally set end time & corrected
    Detect {
        #[arg(long)]
        id: i64,
        #[arg(long)]
        detected: bool,
        #[arg(long)]
        end: Option<i64>,
        #[arg(long, default_value_t = false)]
        corrected: bool,
    },
    /// Add impact record
    Impact {
        #[arg(long)]
        event: i64,
        #[arg(long)]
        type_id: i64,
        #[arg(long)]
        positive: bool,
        #[arg(long)]
        notes: Option<String>,
    },
    /// Recalculate progress metrics
    Recalc,
    /// Show event by id
    Show {
        #[arg(long)]
        id: i64,
    },
    /// Assess event via expert questions and simple heuristics
    Assess {
        #[arg(long)]
        event: i64,
        /// JSON like {"src_independent":"yes", ...}
        #[arg(long)]
        answers: Option<String>,
        /// Apply suggestion to truth_events.detected if confident
        #[arg(long, default_value_t = false)]
        apply: bool,
    },
    /// Add a statement to an event
    AddStatement {
        /// Event id (must exist)
        #[arg(long)]
        event: i64,
        /// Statement text
        #[arg(long)]
        text: String,
        /// Additional context
        #[arg(long)]
        context: Option<String>,
        /// Truth score (-1.0 to 1.0)
        #[arg(long)]
        score: Option<f32>,
    },
    /// Show statement by id
    ShowStatement {
        #[arg(long)]
        id: i64,
    },
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    println!("Truth Training App started!");
    let cli = Cli::parse();
    let mut conn = open_db(&cli.db)?;
    init_db(&conn)?; // idempotent
    match cli.cmd {
        Commands::Init => {
            println!("DB initialized at {}", &cli.db);
        }
        Commands::Seed { locale } => {
            seed_knowledge_base(&mut conn, &locale)?;
            println!("Knowledge base seeded for locale: {}", locale);
        }
        Commands::AddEvent {
            description,
            context,
            vector,
            start,
        } => {
            let id = add_truth_event(
                &conn,
                NewTruthEvent {
                    description,
                    context_id: context,
                    vector,
                    timestamp_start: start.unwrap_or_else(|| Utc::now().timestamp()),
                    code: 1, // Default code for CLI added events
                },
            )?;
            println!("Inserted event id={}", id);
        }
        Commands::Detect {
            id,
            detected,
            end,
            corrected,
        } => {
            set_event_detected(
                &conn,
                id,
                detected,
                end.or_else(|| Some(Utc::now().timestamp())),
                corrected,
            )?;
            println!(
                "Event {} updated (detected={}, corrected={})",
                id, detected, corrected
            );
        }
        Commands::Impact {
            event,
            type_id,
            positive,
            notes,
        } => {
            let iid = add_impact(&conn, event, type_id, positive, notes)?;
            println!("Inserted impact id={} for event={}", iid, event);
        }
        Commands::Recalc => {
            let pm_id = recalc_progress_metrics(&conn, Utc::now().timestamp())?;
            println!("Inserted progress_metrics id={}", pm_id);
        }
        Commands::Show { id } => {
            let ev = get_truth_event(&conn, id)?;
            match ev {
                Some(e) => println!("{:#?}", e),
                None => println!("Event {} not found", id),
            }
        }
        Commands::Assess {
            event,
            answers,
            apply,
        } => {
            cmd_assess(&conn, event, answers, apply)?;
        }
        Commands::AddStatement {
            event,
            text,
            context,
            score,
        } => {
            let id = add_statement(
                &conn,
                NewStatement {
                    event_id: event,
                    text,
                    context,
                    truth_score: score,
                },
            )?;
            println!("Inserted statement id={}", id);
        }
        Commands::ShowStatement { id } => {
            let stmt = get_statement(&conn, id)?;
            match stmt {
                Some(s) => println!("{:#?}", s),
                None => println!("Statement {} not found", id),
            }
        }
    }

    fn cmd_assess(
        conn: &rusqlite::Connection,
        event_id: i64,
        answers_json: Option<String>,
        apply: bool,
    ) -> Result<(), Box<dyn std::error::Error>> {
        // 1) Получаем context.name по event_id
        let (context_name,): (String,) = conn.query_row(
            "SELECT c.name
             FROM truth_events e
             JOIN context c ON c.id = e.context_id
             WHERE e.id=?1",
            rusqlite::params![event_id],
            |row| Ok((row.get::<_, String>(0)?,)),
        )?;

        // 2) Формируем вопросы
        let qs = questions_for_context(&context_name);

        // 3) Если answers не переданы — печатаем пример и выходим
        if answers_json.is_none() {
            println!("Context: {}", context_name);
            println!("Questions (answer with yes/no/unknown or 1..5 for scale):");
            for q in &qs {
                println!(
                    "- {} [{}] (weight={:.2}, truth_bias={})",
                    q.text,
                    match q.kind {
                        core_lib::expert_simple::QuestionKind::YesNo => "yes/no",
                        core_lib::expert_simple::QuestionKind::TriState => "yes/no/unknown",
                        core_lib::expert_simple::QuestionKind::Scale1to5 => "1..5",
                    },
                    q.weight,
                    q.truth_bias
                );
                println!("  id: {}", q.id);
            }
            println!(
                "\nRun again with --answers '{{\"src_independent\":\"yes\",...}}' to evaluate."
            );
            return Ok(());
        }

        // 4) Парсим ответы
        let raw: HashMap<String, String> = serde_json::from_str(&answers_json.unwrap())?;
        let suggestion = evaluate_answers(&qs, &raw);
        println!(
            "Suggestion: score={:.2}, confidence={:.2}, detected_hint={:?}",
            suggestion.score, suggestion.confidence, suggestion.suggested_detected
        );
        println!("Rationale: {}", suggestion.rationale);

        // 5) Применяем (по желанию)
        if apply {
            if suggestion.suggested_detected == Some(true) {
                conn.execute(
                    "UPDATE truth_events SET detected=1 WHERE id=?1",
                    rusqlite::params![event_id],
                )?;
                println!("Applied: detected=TRUE for event {}", event_id);
            } else {
                println!("Not applied: confidence too low or no positive hint.");
            }
        }

        Ok(())
    }

    Ok(())
}
