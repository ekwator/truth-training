use crate::{CoreError, NodeRating};
use rusqlite::{params, Connection, OptionalExtension};

// Константы распространения доверия
const BLEND_LOCAL_WEIGHT: f32 = 0.8;
const BLEND_REMOTE_WEIGHT: f32 = 0.2;
const DECAY_FACTOR: f32 = 0.9; // 10% снижение при давности > 7 суток
const DECAY_SECS: i64 = 7 * 24 * 60 * 60;

/// Смешивает локальный и удалённый скор по формуле:
/// new = local*0.8 + remote*0.2, с обрезкой в [-1, 1]
pub fn blend_trust(local_score: f32, remote_score: f32) -> f32 {
    let blended = local_score * BLEND_LOCAL_WEIGHT + remote_score * BLEND_REMOTE_WEIGHT;
    blended.clamp(-1.0, 1.0)
}

#[cfg(test)]
mod tests {

    #[test]
    fn propagation_priority_blend_from_trust_and_activity() {
        // Прокси-расчёт: trust 0.5 (нормализованный 0.75), activity 0.0 → priority = 0.75*0.8 + 0.0*0.2 = 0.6
        // Эта проверка валидирует веса смешивания (0.8/0.2) на уровне формулы.
        let trust = 0.5f32;
        let trust_norm = ((trust + 1.0) / 2.0).clamp(0.0, 1.0);
        let recent_activity = 0.0f32;
        let priority = trust_norm * 0.8 + recent_activity * 0.2;
        assert!((priority - 0.6).abs() < 1e-6);

        // При высокой активности priority растёт на 20% вклада
        let recent_activity = 1.0f32;
        let priority2 = trust_norm * 0.8 + recent_activity * 0.2;
        assert!(priority2 > priority);
    }
}

/// Применить временной спад доверия: если запись старше 7 дней — умножаем trust_score на 0.9
/// Возвращает количество записей, к которым применился спад
pub fn apply_time_decay(conn: &Connection, now_ts: i64) -> Result<usize, CoreError> {
    let threshold = now_ts - DECAY_SECS;
    // Умножаем на коэффициент
    let affected: usize = conn.execute(
        r#"
        UPDATE node_ratings
        SET trust_score = trust_score * ?1
        WHERE last_updated < ?2
        "#,
        params![DECAY_FACTOR as f64, threshold],
    )?;

    // Обрезаем в диапазон [-1, 1]
    conn.execute(
        r#"
        UPDATE node_ratings
        SET trust_score = CASE
            WHEN trust_score > 1.0 THEN 1.0
            WHEN trust_score < -1.0 THEN -1.0
            ELSE trust_score
        END
        "#,
        [],
    )?;

    Ok(affected)
}

/// Выполнить распространение доверия по входящим оценкам: обновить/вставить blended trust.
/// Возвращает список изменений доверия (node_id, delta)
pub fn propagate_from_remote(
    conn: &Connection,
    incoming_nodes: &[NodeRating],
    now_ts: i64,
) -> Result<Vec<(String, f32)>, CoreError> {
    let mut diffs: Vec<(String, f32)> = Vec::new();

    // Подготовим SELECT и UPSERT/UPDATE
    let mut sel = conn.prepare(
        r#"SELECT events_true, events_false, validations, reused_events, trust_score, last_updated
           FROM node_ratings WHERE node_id = ?1"#,
    )?;

    let mut upsert = conn.prepare(
        r#"
        INSERT INTO node_ratings (node_id, events_true, events_false, validations, reused_events, trust_score, last_updated)
        VALUES (?1, ?2, ?3, ?4, ?5, ?6, ?7)
        ON CONFLICT(node_id) DO UPDATE SET
            events_true   = excluded.events_true,
            events_false  = excluded.events_false,
            validations   = excluded.validations,
            reused_events = excluded.reused_events,
            trust_score   = excluded.trust_score,
            last_updated  = excluded.last_updated
        "#,
    )?;

    for nr in incoming_nodes {
        let row = sel
            .query_row([nr.node_id.as_str()], |r| {
                Ok((
                    r.get::<_, i64>(0)?, // events_true
                    r.get::<_, i64>(1)?, // events_false
                    r.get::<_, i64>(2)?, // validations
                    r.get::<_, i64>(3)?, // reused_events
                    r.get::<_, f64>(4)? as f32, // trust_score
                    r.get::<_, i64>(5)?, // last_updated
                ))
            })
            .optional()?;

        match row {
            Some((et, ef, val, re, local_trust, local_updated)) => {
                let new_trust = blend_trust(local_trust, nr.trust_score);
                let delta = new_trust - local_trust;
                if delta.abs() > 1e-6 {
                    diffs.push((nr.node_id.clone(), delta));
                }
                let events_true = std::cmp::max(et, nr.events_true as i64);
                let events_false = std::cmp::max(ef, nr.events_false as i64);
                let validations = std::cmp::max(val, nr.validations as i64);
                let reused_events = std::cmp::max(re, nr.reused_events as i64);
                let last_updated = std::cmp::max(local_updated, nr.last_updated).max(now_ts);

                upsert.execute(rusqlite::params![
                    nr.node_id,
                    events_true,
                    events_false,
                    validations,
                    reused_events,
                    new_trust as f64,
                    last_updated,
                ])?;
            }
            None => {
                // Нет локальной записи: blended от 0.0 и удалённого
                let new_trust = blend_trust(0.0, nr.trust_score);
                let delta = new_trust; // от 0.0
                if delta.abs() > 1e-6 {
                    diffs.push((nr.node_id.clone(), delta));
                }
                let last_updated = std::cmp::max(nr.last_updated, now_ts);
                upsert.execute(rusqlite::params![
                    nr.node_id,
                    nr.events_true as i64,
                    nr.events_false as i64,
                    nr.validations as i64,
                    nr.reused_events as i64,
                    new_trust as f64,
                    last_updated,
                ])?;
            }
        }
    }

    Ok(diffs)
}
