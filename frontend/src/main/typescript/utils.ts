import { DateTime } from "luxon";
import {
  MappedTag,
  MinimalTag,
  NicoVideoWithError,
  NicoVideoWithMappedTags,
  NicoVideoWithTidyTags,
  Publisher,
  ReleaseEventForApiContractSimplified,
  ReleaseEventForDisplay,
  SongForApiContractSimplified,
  SongForApiContractSimplifiedWithReleaseEvent
} from "@/backend/dto";

// url generators
export function getNicoTagUrl(tag: string, scope: string): string {
  if (scope.length > 0) {
    return "https://nicovideo.jp/tag/" + tag + " " + scope;
  }
  return "https://nicovideo.jp/tag/" + tag;
}

export function getNicoVideoUrl(contentId: string): string {
  return "https://nicovideo.jp/watch/" + contentId;
}

export function getNicoEmbedUrl(videoId: string): string {
  return "https://embed.nicovideo.jp/watch/" + videoId + "?noRelatedVideo=1&enablejsapi=0";
}

export function getVocaDBEventUrl(dbAddress: string, id: number, urlSlug: string): string {
  return dbAddress + "/E/" + id + "/" + urlSlug;
}

export function getVocaDBEntryUrl(dbAddress: string, id: number): string {
  return dbAddress + "/S/" + id;
}

export function getVocaDBTagUrl(dbAddress: string, id: number, urlSlug: string): string {
  return dbAddress + "/T/" + id + "/" + urlSlug;
}

export function getVocaDBAddSongUrl(dbAddress: string, pvLink: string): string {
  return dbAddress + "/Song/Create?pvUrl=https://www.nicovideo.jp/watch/" + pvLink;
}

export function getVocaDBArtistUrl(dbAddress: string, artistId: number): string {
  return dbAddress + "/Ar/" + artistId;
}

export function getDeletedVideoUrl(videoId: string): string {
  return "https://nicolog.jp/watch/" + videoId;
}

// common predicates
export function pageStateIsValid(pageToJump: number, maxPage: number): boolean {
  return pageToJump > 0 && pageToJump <= maxPage;
}

export function infoLoaded(listOfMediaLength: number, frozenTextValue: string): boolean {
  return listOfMediaLength > 0 && frozenTextValue != "";
}

export function allVideosInvisible(list: { rowVisible: boolean }[]): boolean {
  return list.every(item => !item.rowVisible);
}

// common getters
export function getMaxResultsForDisplay(maxResults: number): string {
  return "Results per page: " + maxResults;
}

export function getOrderingConditionForDisplay(orderingCondition: string): string {
  return "Arrange by: " + orderOptions[orderingCondition];
}

export function getSortingConditionForDisplayNico(orderingCondition: string): string {
  return "Arrange by: " + orderOptionsNico[orderingCondition];
}

export function getSortingConditionForDisplay(sortingCondidion: string): string {
  return "Sorting: " + sortOptions[sortingCondidion];
}

export function getAdditionModeForDisplay(additionMode: string): string {
  return "Addition mode: " + additionOptions[additionMode];
}

export function getUniqueElementId(prefix: string, key: string): string {
  return prefix + key;
}

export function getTagVariant(tag: MappedTag, tagsToAssign: MinimalTag[]): string {
  if (tag.assigned) {
    return "success";
  } else if (tagsToAssign.find(t => t.id == tag.tag.id) != undefined) {
    return "warning";
  } else {
    return "outline-success";
  }
}

export function getShortenedSongType(typeString: string): string {
  if (typeString == "Unspecified") {
    return "?";
  } else if (typeString == "MusicPV") {
    return "PV";
  } else {
    return typeString[0];
  }
}

export function getShortenedArtistType(typeString: string): string {
  if (typeString == "Unspecified") {
    return "?";
  } else if (typeString == "SynthesizerV") {
    return "SV";
  } else if (
    typeString == "Vocaloid" ||
    typeString == "UTAU" ||
    typeString == "CeVIO" ||
    typeString == "OtherVoiceSynthesizer" ||
    typeString == "OtherVocalist"
  ) {
    return typeString[0];
  } else {
    return "";
  }
}

export function getDateDisposition(
  date: DateTime | null,
  dateStart: DateTime,
  dateEnd: DateTime | null,
  timeDeltaMax: number
): DateComparisonResult {
  if (date == null) {
    return {
      disposition: "unknown",
      dayDiff: 0,
      eligible: false,
      participatedOnUpload: false,
      participated: false,
      multiple: false
    };
  }

  const dayDiff = subDates(date, dateStart);
  if (dateEnd === null) {
    if (dayDiff === 0) {
      return {
        dayDiff: 0,
        disposition: "perfect",
        eligible: true,
        participatedOnUpload: false,
        participated: false,
        multiple: false
      };
    } else {
      return {
        dayDiff: Math.abs(dayDiff),
        disposition: dayDiff > 0 ? "late" : dayDiff < 0 ? "early" : "perfect",
        eligible: Math.abs(dayDiff) <= timeDeltaMax,
        participatedOnUpload: false,
        participated: false,
        multiple: false
      };
    }
  }

  if (dateStart <= date) {
    if (dateEnd >= date) {
      return {
        dayDiff: 0,
        disposition: "perfect",
        eligible: true,
        participatedOnUpload: false,
        participated: false,
        multiple: false
      };
    } else {
      const dayDiff1 = Math.abs(subDates(date, dateEnd));
      if (dayDiff1 > 0) {
        return {
          dayDiff: dayDiff1,
          disposition: "late",
          eligible: dayDiff1 <= timeDeltaMax,
          participatedOnUpload: false,
          participated: false,
          multiple: false
        };
      } else {
        return {
          dayDiff: 0,
          disposition: "perfect",
          eligible: true,
          participatedOnUpload: false,
          participated: false,
          multiple: false
        };
      }
    }
  } else {
    const dayDiff2 = Math.abs(subDates(dateStart, date));
    if (dayDiff2 > 0) {
      return {
        dayDiff: dayDiff2,
        disposition: "early",
        eligible: dayDiff2 <= timeDeltaMax,
        participatedOnUpload: false,
        participated: false,
        multiple: false
      };
    } else {
      return {
        dayDiff: 0,
        disposition: "perfect",
        eligible: true,
        participatedOnUpload: false,
        participated: false,
        multiple: false
      };
    }
  }
}

export function fillReleaseEventForDisplay(
  src: ReleaseEventForApiContractSimplified,
  trg: ReleaseEventForDisplay
): void {
  trg.id = src.id;
  trg.name = src.name;
  trg.urlSlug = src.urlSlug;
  trg.category = src.category;
  trg.date = src.date == null ? null : DateTime.fromISO(src.date, { zone: "utc" });
  trg.endDate = src.endDate == null ? null : DateTime.fromISO(src.endDate, { zone: "utc" });
}

export const defaultScopeTagString: string =
  "-歌ってみた VOCALOID OR UTAU OR CEVIO OR CeVIO_AI OR SYNTHV OR SYNTHESIZERV OR neutrino(歌声合成エンジン) OR DeepVocal OR Alter/Ego OR AlterEgo OR AquesTalk OR AquesTone OR AquesTone2 OR ボカロ OR ボーカロイド OR 合成音声 OR 歌唱合成 OR coefont OR coefont_studio OR VOICELOID OR VOICEROID OR ENUNU OR ソフトウェアシンガー OR VOICEVOX OR VoiSona OR COEROINK OR NNSVS OR ボイパロイド OR Voicing OR AmadeuSY";

// common interface methods & constants
export const orderOptions = {
  PublishDate: "upload time",
  AdditionDate: "addition time",
  RatingScore: "user rating"
};

export const orderOptionsNico = {
  startTime: "upload time",
  viewCounter: "views",
  lengthSeconds: "length"
};

export const sortOptions = {
  CreateDate: "old→new",
  CreateDateDescending: "new→old"
};

export const additionOptions = {
  before: "before",
  since: "since"
};
export const directionMap = {
  before: "Older",
  since: "Newer"
};

export const reverseEachMap = {
  before: {
    CreateDateDescending: {
      Older: false,
      Newer: false
    },
    CreateDate: {
      Older: false,
      Newer: false
    }
  },
  since: {
    CreateDateDescending: {
      Older: true
    },
    CreateDate: {
      Older: true
    }
  }
};

export const reverseAllMap = {
  before: {
    CreateDateDescending: {
      Older: false,
      Newer: true
    },
    CreateDate: {
      Older: false,
      Newer: false
    }
  },
  since: {
    CreateDateDescending: {
      Older: true
    },
    CreateDate: {
      Older: false
    }
  }
};

export const songTypeToTag = {
  Unspecified: [],
  Original: [6479, 22, 74],
  Remaster: [1519, 391, 371, 392, 74],
  Remix: [371, 74, 391, 4709],
  Cover: [74, 371, 392],
  Instrumental: [208],
  MusicPV: [7378, 74, 4582],
  Mashup: [3392],
  DramaPV: [
    104,
    1736,
    7276,
    3180,
    7728,
    8509,
    7748,
    7275,
    6701,
    3186,
    8130,
    6700,
    7615,
    6703,
    6702,
    7988,
    6650,
    8043,
    8409,
    423
  ],
  Other: []
};

export interface EntryAction {
  action:
    | "Assign"
    | "TagWithParticipant"
    | "UpdateDescription"
    | "RemoveEvent"
    | "NoAction"
    | "Untag";
}

export function hasAction(actions: EntryAction[], actionName: string): boolean {
  return actions.map(value => value.action).findIndex(value => value == actionName) > -1;
}

export function hasAnyAction(actions: EntryAction[], actionNames: string[]): boolean {
  return actions.map(value => value.action).some(value => actionNames.includes(value));
}

export function getDescriptionAction(
  actions: EntryAction[],
  song: VideoWithEntryAndVisibility | EntryWithReleaseEventAndVisibility
): string {
  if (
    (actions.map(value => value.action).find(value => value == "UpdateDescription") ??
      "NoAction") == "NoAction"
  ) {
    return "NoAction";
  } else if (song.songEntry != null) {
    if (
      actions.map(value => value.action).findIndex(value => value == "TagWithParticipant") > -1 ||
      song.songEntry.eventDateComparison.eligible ||
      song.songEntry.eventDateComparison.disposition == "early"
    ) {
      return "TagWithParticipant";
    }
  }
  return "NoAction";
}

export function getSongTypeColorForDisplay(typeString: string): string {
  if (typeString == "Original" || typeString == "Remaster") {
    return "primary";
  } else if (
    typeString == "Remix" ||
    typeString == "Cover" ||
    typeString == "Mashup" ||
    typeString == "Other"
  ) {
    return "secondary";
  } else if (typeString == "Instrumental") {
    return "dark";
  } else if (typeString == "MusicPV" || typeString == "DramaPV") {
    return "success";
  } else {
    return "warning";
  }
}

export function getArtistTypeColorForDisplay(typeString: string): string {
  if (typeString == "Vocaloid") {
    return "primary";
  } else if (typeString == "UTAU") {
    return "danger";
  } else if (typeString == "CeVIO") {
    return "success";
  } else if (typeString == "SynthesizerV") {
    return "secondary";
  } else if (typeString == "OtherVoiceSynthesizer") {
    return "dark";
  } else if (typeString == "OtherVocalist") {
    return "secondary";
  } else {
    return "";
  }
}

export function compareWithDelta(dayDiff: number, delta: number): string {
  return dayDiff <= delta ? "warning" : "danger";
}

export function getDispositionBadgeColorVariant(
  eventDateComparison: DateComparisonResult,
  delta: number
): string {
  return eventDateComparison.disposition === "perfect"
    ? "success"
    : eventDateComparison.disposition === "unknown"
    ? "secondary"
    : compareWithDelta(eventDateComparison.dayDiff, delta);
}

export function getEventColorVariant(
  event: ReleaseEventForApiContractSimplified,
  eventId: number,
  participatedOnUpload: boolean
): string {
  if (event.id == eventId) {
    if (!participatedOnUpload) {
      return "success";
    } else {
      return "danger";
    }
  }
  return "warning";
}

export function getSongTypeStatsForDisplay(type: string, number: number): string {
  return type + " (" + number + ")";
}

export function validateTimestamp(timestamp: string): boolean | null {
  if (timestamp === "") {
    return null;
  }
  return DateTime.fromISO(timestamp).isValid;
}

// other util methods
function subDates(date1: DateTime, date2: DateTime): number {
  return Math.floor(date1.diff(date2).as("day"));
}

export function toggleTagAssignation(tag: MappedTag, video: EntryWithVideosAndVisibility): void {
  const assign = video.tagsToAssign.filter((t: MinimalTag) => t.id == tag.tag.id).length > 0;
  if (assign) {
    video.tagsToAssign = video.tagsToAssign.filter((t: MinimalTag) => t.id != tag.tag.id);
  } else {
    video.tagsToAssign.push(tag.tag);
  }
  for (const thumbnailOk of video.thumbnailsOk) {
    for (const mappedTag of thumbnailOk.mappedTags) {
      if (mappedTag.tag.id == tag.tag.id) {
        mappedTag.toAssign = !assign;
      }
    }
    video.toAssign = video.tagsToAssign.length > 0;
  }
}

export function getTagIconForTagAssignationButton(
  tag: MappedTag,
  tagsToAssign: MinimalTag[]
): string[] {
  if (tag.assigned) {
    return ["fas", "fa-check"];
  } else if (tagsToAssign.find(t => t.id == tag.tag.id) != undefined) {
    return ["fas", "fa-minus"];
  } else {
    return ["fas", "fa-plus"];
  }
}

export function getButtonPayload(
  videos: EntryWithVideosAndVisibility[],
  additionMode: string,
  sortingCondition: string,
  direction: string
): Fetch1Payload {
  if (direction == "Older") {
    if (additionMode == "before") {
      if (sortingCondition == "CreateDateDescending") {
        const song = videos[videos.length - 1].song;
        return {
          id: song.id,
          createDate: song.createDate,
          sortRule: "CreateDateDescending",
          mode: "before"
        };
      } else if (sortingCondition == "CreateDate") {
        const song = videos[0].song;
        return {
          id: song.id,
          createDate: song.createDate,
          sortRule: "CreateDateDescending",
          mode: "before"
        };
      }
    } else if (additionMode == "since") {
      if (sortingCondition == "CreateDateDescending") {
        const song = videos[videos.length - 1].song;
        return {
          id: song.id,
          createDate: song.createDate,
          sortRule: "CreateDateDescending",
          mode: "before"
        };
      } else if (sortingCondition == "CreateDate") {
        const song = videos[0].song;
        return {
          id: song.id,
          createDate: song.createDate,
          sortRule: "CreateDateDescending",
          mode: "before"
        };
      }
    }
  } else if (direction == "Newer") {
    if (additionMode == "before") {
      if (sortingCondition == "CreateDateDescending") {
        const song = videos[0].song;
        return {
          id: song.id,
          createDate: song.createDate,
          sortRule: "CreateDate",
          mode: "since"
        };
      } else if (sortingCondition == "CreateDate") {
        const song = videos[videos.length - 1].song;
        return {
          id: song.id,
          createDate: song.createDate,
          sortRule: "CreateDate",
          mode: "since"
        };
      }
    }
  }
  throw {
    response: undefined,
    message:
      "[getButtonPayload] Unexpected arguments: (" +
      additionMode +
      ", " +
      sortingCondition +
      ", " +
      direction +
      ")"
  };
}

export function updateFetch1Payload(
  responseItems: EntryWithVideosAndVisibility[],
  additionMode: string,
  sortingCondition: string,
  timestampNewest: string,
  timestampOldest: string,
  direction: string
): Fetch1Payload {
  if (direction == "Older") {
    if (additionMode == "before") {
      if (sortingCondition == "CreateDateDescending") {
        const song = responseItems[responseItems.length - 1].song;
        return {
          mode: "before",
          createDate: song.createDate,
          sortRule: "CreateDateDescending",
          id: song.id
        };
      } else if (sortingCondition == "CreateDate") {
        const song = responseItems[responseItems.length - 1].song;
        return {
          mode: "since",
          createDate: song.createDate,
          sortRule: "CreateDate",
          id: song.id
        };
      }
    } else if (additionMode == "since") {
      if (sortingCondition == "CreateDate") {
        const song = responseItems[responseItems.length - 1].song;
        return {
          mode: "since",
          createDate: song.createDate,
          sortRule: "CreateDate",
          id: song.id
        };
      } else if (sortingCondition == "CreateDateDescending") {
        const song = responseItems[responseItems.length - 1].song;
        return {
          mode: "before",
          createDate: song.createDate,
          sortRule: "CreateDateDescending",
          id: song.id
        };
      }
    }
  } else if (direction == "Newer") {
    if (additionMode == "since") {
      if (sortingCondition == "CreateDate") {
        const song = responseItems[responseItems.length - 1].song;
        return {
          id: song.id,
          createDate: song.createDate,
          sortRule: "CreateDate",
          mode: "since"
        };
      } else if (sortingCondition == "CreateDateDescending") {
        const song = responseItems[0].song;
        return {
          id: song.id,
          createDate: song.createDate,
          sortRule: "CreateDate",
          mode: "since"
        };
      }
    }
  }
  throw {
    response: undefined,
    message:
      "[getButtonPayload] Unexpected arguments: (" +
      additionMode +
      ", " +
      sortingCondition +
      ", " +
      direction +
      ")"
  };
}

export function dateIsWithinTimeDelta(
  timeDelta: number,
  restrictionBefore: boolean,
  restrictionAfter: boolean,
  dateComparisonResult: DateComparisonResult
): boolean {
  if (dateComparisonResult.disposition == "perfect") {
    return true;
  }

  if (restrictionBefore && restrictionAfter) {
    return dateComparisonResult.dayDiff <= timeDelta;
  } else if (restrictionBefore) {
    return dateComparisonResult.disposition == "late" || dateComparisonResult.dayDiff <= timeDelta;
  } else {
    return dateComparisonResult.disposition == "early" || dateComparisonResult.dayDiff <= timeDelta;
  }
}

export function getTimeDeltaState(
  isEnabled: boolean,
  before: boolean,
  after: boolean,
  delta: number
): boolean {
  if (!isEnabled) {
    return true;
  }
  if (delta < 0) {
    return false;
  }
  return before || after;
}

export function isEligible(
  songEntry: SongForApiContractSimplifiedWithReleaseEvent | null,
  videoUploadDateComparison: DateComparisonResult | null
): boolean {
  return (
    (videoUploadDateComparison != null && videoUploadDateComparison.eligible) ||
    (songEntry != null &&
      songEntry.eventDateComparison != null &&
      songEntry.eventDateComparison.eligible)
  );
}

export function isEarly(
  songEntry: SongForApiContractSimplifiedWithReleaseEvent | null,
  videoUploadDateComparison: DateComparisonResult | null
): boolean {
  if (isEligible(songEntry, videoUploadDateComparison)) {
    return false;
  }
  return (
    (songEntry != null && songEntry.eventDateComparison.disposition == "early") ||
    (videoUploadDateComparison != null && videoUploadDateComparison.disposition == "early")
  );
}

// data structures
export interface EntryWithReleaseEventAndVisibility {
  songEntry: SongForApiContractSimplifiedWithReleaseEvent;
  publishDate: DateTime | null;
  rowVisible: boolean;
  toAssign: boolean;
  processed: boolean;
}

export interface VideoWithEntryAndVisibility {
  video: NicoVideoWithTidyTags;
  songEntry: SongForApiContractSimplifiedWithReleaseEvent | null;
  embedVisible: boolean;
  rowVisible: boolean;
  toAssign: boolean;
  publisher: Publisher | null;
  processed: boolean;
}

export interface SongType {
  name: string;
  show: boolean;
}

export interface Fetch1Payload {
  mode: string;
  createDate: string;
  id: number;
  sortRule: string;
}

export interface EntryWithVideosAndVisibility {
  thumbnailsOk: NicoVideoWithMappedTags[];
  thumbnailsErr: NicoVideoWithError[];
  song: SongForApiContractSimplified;
  visible: boolean;
  toAssign: boolean;
  tagsToAssign: MinimalTag[];
  disable: string[];
}

export interface DateComparisonResult {
  dayDiff: number;
  disposition: "perfect" | "late" | "early" | "unknown";
  eligible: boolean;
  participatedOnUpload: boolean;
  participated: boolean;
  multiple: boolean;
}
