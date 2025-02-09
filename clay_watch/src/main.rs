use hotwatch::{Event, EventKind, Hotwatch};
use std::env;
mod diff;

fn main() {
    /*
    let lines1 = diff::read_lines("./test_data/sepvs/before/DecryptFiles.java");
    let lines2 = diff::read_lines("./test_data/sepvs/after/DecryptFiles.java");
    */
    let lines1 = diff::read_lines("./test_data/before/edit_multiple_lines.txt");
    let lines2 = diff::read_lines("./test_data/after/edit_multiple_lines.txt");

    let diff = diff::lines_diff::<usize>(lines1, lines2);
    for line in diff {
        println!(
            "{} {}",
            match line.0 {
                None => String::from(" "),
                Some(o) => format!("{}", o),
            },
            line.1
        );
    }

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
