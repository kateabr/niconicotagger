export const localStorageKeyClientType = "clientType";
export const localStorageKeyMaxResults = "max_results";
export const localStorageKeyStartOffset = "start_offset";
export const localStorageKeyDbOrderBy = "db_order_by";
export const localStorageKeyNndOrderBy = "nnd_order_by";
export const localStorageKeyEventByNndTagsName = "event_by_nnd_tags_name";
export const localStorageKeyEventByDbTagName = "event_by_db_tag_name";
export const localStorageKeyNndTag = "nnd_mapped_tag";
export const localStorageKeyNndTagScope = "nnd_mapped_tag_scope";
export const localStorageKeyDbTag = "db_mapped_tag";
export const localStorageKeyDbTagScope = "db_mapped_tag_scope";

export const defaultScopeTagString: string =
  "-歌ってみた VOCALOID OR UTAU OR CEVIO OR CeVIO_AI OR SYNTHV OR SYNTHESIZERV OR neutrino(歌声合成エンジン) OR DeepVocal OR Alter/Ego OR AlterEgo OR AquesTalk OR AquesTone OR AquesTone2 OR ボカロ OR ボーカロイド OR 合成音声 OR 歌唱合成 OR coefont OR coefont_studio OR VOICELOID OR VOICEROID OR ENUNU OR ソフトウェアシンガー OR VOICEVOX OR VoiSona OR COEROINK OR NNSVS OR ボイパロイド OR Voicing OR AmadeuSY";

export const maxResultsOptions: number[] = [10, 25, 50, 100];
export const vocaDbOrderOptions = { AdditionDate: "addition time", PublishDate: "upload time" };
export const nndOrderOptions = {
  startTime: "upload time",
  viewCounter: "views",
  likeCounter: "likes"
};
export const dbBaseUrl = {
  vocadb: "https://vocadb.net",
  vocadb_beta: "https://beta.vocadb.net"
};

export const videoStatusesToDisable = ["DELETED"];

export const serviceName = "NicoNicoTagger";
