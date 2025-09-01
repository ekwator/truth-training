#[derive(Debug)]
pub struct TruthEvent {
    pub id: u64,
    pub description: String,
    pub context: String,
    pub detected: bool, // true если ложь распознана
    pub timestamp: i64, // UNIX time
}

pub fn add_truth_event(event: TruthEvent) {
    println!("New event added: {:?}", event);
}
