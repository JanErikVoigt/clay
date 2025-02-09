use super::longest_common_subsequence::{self, longest_common_subsequence};
use std::collections::HashMap;
use std::fmt;
use std::fs::read_to_string;

pub fn read_lines(filename: &str) -> Vec<String> {
    let mut result = Vec::new();

    for line in read_to_string(filename).unwrap().lines() {
        result.push(line.to_string())
    }

    result
}

pub enum LineOperation {
    Add,
    Remove,
}

impl fmt::Display for LineOperation {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(
            f,
            "{:?}",
            match self {
                LineOperation::Add => "+",
                LineOperation::Remove => "-",
                _ => "?",
            }
        )
    }
}

//TODO is usize the right call?
// goal: delete as early as possible, add as late as possible

pub fn lines_diff<N>(
    lines1: Vec<String>,
    lines2: Vec<String>,
) -> Vec<(Option<LineOperation>, String)>
where
    N: num::PrimInt + std::iter::Sum,
{
    //index lines... unique line -> new index!

    //TODO reserve space
    let mut unique_line_enumeration = HashMap::<String, usize>::new();
    let mut lines1_ids = Vec::new();
    let mut lines2_ids = Vec::new();

    for line in &lines1 {
        let result = unique_line_enumeration.get_key_value(line);

        if result.is_some() {
            lines1_ids.push(*result.unwrap().1);
        } else {
            let new_id = unique_line_enumeration.len();
            lines1_ids.push(new_id);
            unique_line_enumeration.insert(line.clone(), new_id);
        }
    }

    //TODO repetitive code
    for line in &lines2 {
        let result = unique_line_enumeration.get_key_value(line);

        if result.is_some() {
            lines2_ids.push(*result.unwrap().1);
        } else {
            let new_id = unique_line_enumeration.len();
            lines2_ids.push(new_id);
            unique_line_enumeration.insert(line.clone(), new_id);
        }
    }

    let lcssq: Vec<usize> = longest_common_subsequence(&lines1_ids, &lines2_ids);
    let mut result_ints = Vec::new();

    let mut common_i = 0;
    let mut seq2_i = 0;
    let mut seq2_is_common = Vec::new();
    for _ in 0..lines2_ids.len() {
        seq2_is_common.push(false);
    }
    while seq2_i < lines2_ids.len() {
        if lcssq[common_i] == lines2_ids[seq2_i] {
            seq2_is_common[seq2_i] = true;

            common_i += 1;
            seq2_i += 1;
        } else {
            seq2_i += 1;
        }
    }
    assert!(
        common_i == lcssq.len() - 1 + 1,
        "common should be traversed ({} != {})",
        common_i,
        lcssq.len() - 1 + 1,
    );

    common_i = lcssq.len() - 1;
    let mut seq1_i = lines1_ids.len() - 1;
    let mut seq1_is_common = Vec::new();
    for _ in 0..lines1_ids.len() {
        seq1_is_common.push(false);
    }
    loop {
        if lcssq[common_i] == lines1_ids[seq1_i] {
            seq1_is_common[seq1_i] = true;
            if (seq1_i == 0) {
                break;
            }
            common_i -= 1;
            seq1_i -= 1;
        } else {
            seq1_i -= 1;
        }
    }
    assert!(common_i == 0);

    /*for i in 0..lines1_ids.len().max(lines2_ids.len()) {
        println!(
            "{}({:?}) and {}({}) makes {}",
            match lines1_ids.get(i) {
                None => String::from(" "),
                Some(v) => format!("{}", v),
            },
            seq1_is_common[i],
            match lines2_ids.get(i) {
                None => String::from(" "),
                Some(v) => format!("{}", v),
            },
            seq2_is_common[i],
            match lcssq.get(i) {
                None => String::from(" "),
                Some(v) => format!("{}", v),
            }
        )
    }*/

    // let mut seq1_i = 0;
    // let mut seq2_i = 0;
    // //let mut comm_i = 0;

    // while !(seq1_i >= lines1_ids.len() && seq1_i >= lines1_ids.len()) { //&& comm_i >= lcssq.len()

    // }

    let mut seq1_it = lines1_ids.iter().enumerate().peekable();
    let mut seq2_it = lines2_ids.iter().enumerate().peekable();

    for common_element in lcssq.iter() {
        let seq1_front = seq1_it.peek();
        let seq2_front = seq2_it.peek();
        if seq1_front.is_some() && seq1_is_common[seq1_front.unwrap().0] {
            if seq2_front.is_some() && seq2_is_common[seq2_front.unwrap().0] {
                //all equal!
                //println!("all equal");
                result_ints.push((None, *common_element));
                let _ = seq1_it.next();
                let _ = seq2_it.next();
                continue;
            }
        }
        while seq1_it.peek().is_some() && *seq1_it.peek().unwrap() != common_element {
            result_ints.push((Some(LineOperation::Remove), **seq1_it.peek().unwrap()));
            let _ = seq1_it.next();
        }
        while seq2_it.peek().is_some() && *seq2_it.peek().unwrap() != common_element {
            result_ints.push((Some(LineOperation::Add), **seq2_it.peek().unwrap()));
            let _ = seq2_it.next();
        }
    }

    //println!("{:?} \n {:?}\n {:?}", lines1_ids, lines2_ids, lcssq);

    let mut id_to_line = Vec::new();
    for (line, id) in unique_line_enumeration.into_iter() {
        while id_to_line.len() <= id {
            id_to_line.push(None);
        }
        id_to_line[id] = Some(line);
    }

    let result_strings = result_ints
        .into_iter()
        .map(|(op, i)| (op, id_to_line[i].clone().unwrap()))
        .collect();

    return result_strings;
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn lines_diff_DecryptFiles_java() {
        let lines1 = read_lines("./test_data/sepvs/before/DecryptFiles.java");
        let lines2 = read_lines("./test_data/sepvs/after/DecryptFiles.java");

        let diff = lines_diff::<usize>(lines1, lines2);
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
        //assert_eq!(lines_diff(&first_seq, &second_seq), expected_lcs);
    }

    /*macro_rules! lines_diff_tests {
        ($($name:ident: $test_case:expr,)*) => {
            $(
                #[test]
                fn $name() {
                    let (first_seq, second_seq, expected_lcs) = $test_case;
                    assert_eq!(lines_diff(&first_seq, &second_seq), expected_lcs);
                }
            )*
        };
    }

    lines_diff_tests! {
        empty_case: (Vec::<u32>::new(), Vec::<u32>::new(), Vec::<u32>::new() ),
        one_empty: (vec![], vec![0,1,2,3],vec![]),
        identical_strings: (vec![0,1,2,3], vec![0,1,2,3], vec![0,1,2,3]),
        completely_different: (vec![0,1,2,3], vec![10,11,21,13], vec![]),
        single_character: (vec![42], vec![42], vec![42]),
        /*different_length: ("abcd", "abc", "abc"),
        special_characters: ("$#%&", "#@!%", "#%"),
        long_strings: ("abcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefgh",
                      "bcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefgha",
                      "bcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefghabcdefgh"),
        unicode_characters: ("你好，世界", "再见，世界", "，世界"),
        spaces_and_punctuation_0: ("hello, world!", "world, hello!", "hello!"),
        spaces_and_punctuation_1: ("hello, world!", "world, hello!", "hello!"), // longest_common_subsequence is not symmetric
        random_case_1: ("abcdef", "xbcxxxe", "bce"),
        random_case_2: ("xyz", "abc", ""),
        random_case_3: ("abracadabra", "avadakedavra", "aaadara"),*/
    }*/
}

/* TEST CASE:
should match common 2 with last two before 30...
goal: delete as early as possible, add as late as possible
28 and 28 makes 28
2 and 163 makes 2
29 and 2 makes 30
2 and 30 makes 31
2 and 31 makes 2
30 and 2 makes 32
31 and 32 makes 33
2 and 33 makes 34
2 and 34 makes 35
2 and 35 makes 36
32 and 36 makes 37
33 and 37 makes 38


public static void main(String[] args) throws IOException {
"+"         int which_demo = -1;
"-"
"-"         int which_demo = 4;
"-"
"-"
"+"
"-"         final List<String> ciphertextDirectories;
"+"         final List<String> ciphertextDirectories;
"-"         final List<String> decryptionDirectories;
"+"         final List<St
*/
