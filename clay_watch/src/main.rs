use hotwatch::{Event, EventKind, Hotwatch};
use std::env;

fn main() {
    let args: Vec<String> = env::args().collect();

    let mut hotwatch = Hotwatch::new().expect("hotwatch failed to initialize!");
    hotwatch
        .watch("../repo_demo", |event: Event| {
            if let EventKind::Modify(_) = event.kind {
                println!("{:?} changed!", event.paths[0]);
            }
        })
        .expect("failed to watch file!");

    while (true) {}
}
