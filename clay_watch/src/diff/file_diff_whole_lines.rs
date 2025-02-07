use super::longest_common_subsequence::{self, longest_common_subsequence};
use std::collections::HashMap;
use std::fs::read_to_string;

fn read_lines(filename: &str) -> Vec<String> {
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

//TODO is usize the right call?
pub fn lines_diff<N>(lines1: Vec<String>, lines2: Vec<String>) -> Vec<(LineOperation, String)>
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

    let lcssq = longest_common_subsequence(&lines1, &lines2);

    let result_ints = Vec::new();

    let result_strings = Vec::new();
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn lines_diff_DecryptFiles_java() {
        let lines1 = read_lines("./test_data/sepvs/before/DecryptFiles.java");
        let lines2 = read_lines("./test_data/sepvs/after/DecryptFiles.java");

        println!(lines_diff(lines1, lines2));
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
