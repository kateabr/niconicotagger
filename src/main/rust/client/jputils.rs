use kana::{half2kana, kata2hira, wide2ascii};

pub fn normalize(tag: &str) -> String {
    let ascii = wide2ascii(tag);
    let kana = half2kana(ascii.as_str());
    let hira = kata2hira(kana.as_str());
    return hira.to_lowercase();
}
