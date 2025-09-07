export type DbSortOrder = "AdditionDate" | "PublishDate";
export type NndTagType = "primary" | "info" | "dark" | "secondary";
export type QueryConsoleApiType = "songs" | "artists";
export type NndSortOrder = "viewCounter" | "startTime" | "likeCounter";
export type PublisherType = "NND_USER" | "NND_CHANNEL" | "DATABASE";
export type EventStatus = "ENDED" | "ONGOING" | "UPCOMING" | "ENDLESS";

export enum SongType {
  Unspecified = 0,
  Original = 1,
  Remaster = 2,
  Remix = 3,
  Cover = 4,
  Instrumental = 5,
  MusicPV = 6,
  Mashup = 7,
  DramaPV = 8,
  Other = 9
}
export type ReleaseEventCategory =
  | "Unspecified"
  | "AlbumRelease"
  | "Anniversary"
  | "Club"
  | "Concert"
  | "Contest"
  | "Convention"
  | "Other"
  | "Festival";
export type ArtistType =
  | "Unknown"
  | "Circle"
  | "Label"
  | "Producer"
  | "Animator"
  | "Illustrator"
  | "Lyricist"
  | "Vocaloid"
  | "UTAU"
  | "CeVIO"
  | "Other"
  | "OtherVoiceSynthesizer"
  | "OtherVocalist"
  | "OtherGroup"
  | "OtherIndividual"
  | "Utaite"
  | "Band"
  | "Vocalist"
  | "Character"
  | "SynthesizerV"
  | "CoverArtist"
  | "NEUTRINO"
  | "VoiSona"
  | "NewType"
  | "Voiceroid"
  | "Instrumentalist"
  | "Designer"
  | "VOICEVOX"
  | "ACEVirtualSinger"
  | "AIVOICE";
