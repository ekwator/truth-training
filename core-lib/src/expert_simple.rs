use serde::{Deserialize, Serialize};
use std::collections::HashMap;

/// Тип вопроса
#[derive(Debug, Clone, Serialize, Deserialize)]
pub enum QuestionKind {
    YesNo,     // "yes" | "no"
    TriState,  // "yes" | "no" | "unknown"
    Scale1to5, // "1".."5"
}

/// Универсальный вопрос
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Question {
    pub id: String,
    pub text: String,
    pub kind: QuestionKind,
    /// Вес вопроса в суммарном балле (-1..+1)
    pub weight: f32,
    /// Ожидаемое “правдоустанавливающее” направление:
    /// true  => “да/выше” обычно поддерживает правду,
    /// false => “да/выше” обычно поддерживает ложь.
    pub truth_bias: bool,
}

/// Ответы пользователя: map<question_id, string value>
pub type Answers = HashMap<String, String>;

/// Результат простой эвристики
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Suggestion {
    pub suggested_detected: Option<bool>, // None если уверенности мало
    pub confidence: f32,                  // 0..1
    pub score: f32,                       // -1..+1 (от “похоже на ложь” к “похоже на правду”)
    pub rationale: String,                // краткое объяснение
}

/// Набор универсальных вопросов.
/// Можно варьировать тексты под контекстом через name, но состав — общий.
pub fn questions_for_context(_context_name: &str) -> Vec<Question> {
    vec![
        Question {
            id: "src_independent".into(),
            text: "Есть ли независимые источники/свидетельства, подтверждающие факт?".into(),
            kind: QuestionKind::TriState,
            weight: 0.22,
            truth_bias: true,
        },
        Question {
            id: "alt_hypothesis".into(),
            text: "Были ли рассмотрены альтернативные объяснения (обновления системы, интерфейсные изменения, задержки)?".into(),
            kind: QuestionKind::TriState,
            weight: 0.18,
            truth_bias: true,
        },
        Question {
            id: "incentives".into(),
            text: "Есть ли у предполагаемого субъекта явная выгода от искажения фактов?".into(),
            kind: QuestionKind::TriState,
            weight: 0.20,
            truth_bias: false, // “да” усиливает версию лжи
        },
        Question {
            id: "reproducible".into(),
            text: "Явление воспроизводимо при повторной проверке в контролируемых условиях?".into(),
            kind: QuestionKind::TriState,
            weight: 0.16,
            truth_bias: true,
        },
        Question {
            id: "logs_evidence".into(),
            text: "Есть ли технические/документальные подтверждения (логи, таймстемпы, скриншоты)?".into(),
            kind: QuestionKind::TriState,
            weight: 0.18,
            truth_bias: true,
        },
        Question {
            id: "belief_pressure".into(),
            text: "Влияли ли религиозные/философские убеждения или предрассудки на вывод (ощущаю давление «как должно быть»)?".into(),
            kind: QuestionKind::TriState,
            weight: 0.12,
            truth_bias: false, // “да” — повышает риск ошибки/лжи
        },
        Question {
            id: "time_distance".into(),
            text: "Оценка сделана не «сгоряча» (была пауза, пересмотр контекста, проверка фактов)?".into(),
            kind: QuestionKind::TriState,
            weight: 0.10,
            truth_bias: true,
        },
    ]
}

/// Преобразуем ответ в числовое значение с учётом типа
fn value_to_num(kind: &QuestionKind, raw: &str) -> Option<f32> {
    match kind {
        QuestionKind::YesNo => match raw {
            "yes" => Some(1.0),
            "no" => Some(0.0),
            _ => None,
        },
        QuestionKind::TriState => match raw {
            "yes" => Some(1.0),
            "no" => Some(0.0),
            "unknown" => Some(0.5),
            _ => None,
        },
        QuestionKind::Scale1to5 => raw.parse::<f32>().ok().map(|v| (v - 1.0) / 4.0), // 1..5 -> 0..1
    }
}

/// Простая эвристика вычисления score/confidence и подсказки detected
pub fn evaluate_answers(questions: &[Question], answers: &Answers) -> Suggestion {
    let mut total_weight = 0.0;
    let mut acc = 0.0;
    let mut answered = 0;

    for q in questions {
        if let Some(raw) = answers.get(&q.id)
            && let Some(mut x) = value_to_num(&q.kind, raw)
        {
            // Направление: если truth_bias=false, инвертируем вклад
            if !q.truth_bias {
                x = 1.0 - x;
            }
            acc += (x - 0.5) * 2.0 * q.weight; // нормируем к [-1..+1] на вес
            total_weight += q.weight;
            answered += 1;
        }
    }

    let score = if total_weight > 0.0 {
        (acc / total_weight).clamp(-1.0, 1.0)
    } else {
        0.0
    };
    // Уверенность: доля отвеченных * “качество” ответов (штрафуем за unknown)
    let answered_ratio = if questions.is_empty() {
        0.0
    } else {
        answered as f32 / questions.len() as f32
    };
    let unknown_penalty = answers.values().filter(|v| v.as_str() == "unknown").count() as f32
        / answers.len().max(1) as f32;
    let mut confidence = (answered_ratio * (1.0 - 0.6 * unknown_penalty)).clamp(0.0, 1.0);

    // Подсказка по detected: вводим мягкий порог
    // score >= 0 → “скорее правда”, <0 → “скорее ложь”
    let suggested_detected = if confidence >= 0.6 {
        Some(true) // событие “достаточно определено”
    } else {
        None
    };

    // Подкручиваем confidence: чем дальше score от 0, тем выше уверенность
    confidence = ((confidence + (score.abs() * 0.5)).min(1.0)).clamp(0.0, 1.0);

    let rationale = if score >= 0.2 {
        "Ответы в сумме поддерживают правдоподобность и фактологическую проверку.".to_string()
    } else if score <= -0.2 {
        "Ответы в сумме указывают на вероятное искажение/предвзятость либо отсутствие подтверждений.".to_string()
    } else {
        "Картина неоднозначна: есть признаки как в пользу правды, так и в пользу заблуждения/лжи."
            .to_string()
    };

    Suggestion {
        suggested_detected,
        confidence,
        score,
        rationale,
    }
}
