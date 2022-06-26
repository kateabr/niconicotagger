import { DateTime } from "luxon";
import {
  DateComparisonResult,
  MappedTag,
  MinimalTag,
  NicoVideoWithError,
  NicoVideoWithMappedTags,
  NicoVideoWithTidyTags,
  Publisher,
  ReleaseEventForApiContractSimplified,
  ReleaseEventForApiContractSimplifiedWithNndTags,
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

export function getVocaDBEventUrl(id: number, urlSlug: string): string {
  return "https://vocadb.net/E/" + id + "/" + urlSlug;
}

export function getVocaDBEntryUrl(id: number): string {
  return "https://vocadb.net/S/" + id;
}

export function getVocaDBTagUrl(id: number, urlSlug: string): string {
  return "https://vocadb.net/T/" + id + "/" + urlSlug;
}

export function getVocaDBAddSongUrl(pvLink: string): string {
  return "https://vocadb.net/Song/Create?PVUrl=https://www.nicovideo.jp/watch/" + pvLink;
}

export function getVocaDBArtistUrl(artistId: number): string {
  return "https://vocadb.net/Ar/" + artistId;
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

export function getDateDisposition(
  date: DateTime | null,
  dateStart: DateTime,
  dateEnd: DateTime | null
): DateComparisonResult {
  if (date == null) {
    return { disposition: "unknown", dayDiff: 0 };
  }

  const dayDiff = subDates(date, dateStart);
  if (dateEnd === null) {
    if (dayDiff === 0) {
      return { dayDiff: 0, disposition: "perfect" };
    } else {
      return {
        dayDiff: Math.round(dayDiff),
        disposition: dayDiff > 0 ? "late" : "early"
      };
    }
  }

  if (dateStart <= date) {
    if (dateEnd >= date) {
      return { dayDiff: 0, disposition: "perfect" };
    } else {
      return {
        dayDiff: Math.round(subDates(date, dateEnd)),
        disposition: "late"
      };
    }
  } else {
    return {
      dayDiff: Math.round(subDates(dateStart, date)),
      disposition: "early"
    };
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
  trg.date = src.date == null ? null : DateTime.fromISO(src.date);
  trg.endDate = src.endDate == null ? null : DateTime.fromISO(src.endDate);
}

export const defaultScopeTagString: string =
  "-歌ってみた VOCALOID OR UTAU OR CEVIO OR SYNTHV OR SYNTHESIZERV OR neutrino(歌声合成エンジン) OR DeepVocal OR Alter/Ego OR AlterEgo OR AquesTalk OR AquesTone OR AquesTone2 OR ボカロ OR ボーカロイド OR 合成音声 OR 歌唱合成 OR coefont OR coefont_studio OR VOICELOID OR VOICEROID OR ENUNU OR ソフトウェアシンガー";

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

export function getDispositionBadgeColorVariant(disposition: string): string {
  return disposition === "perfect"
    ? "success"
    : disposition === "unknown"
    ? "secondary"
    : "warning";
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
  return date1.diff(date2).as("day");
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

export function getRightButtonPayload(
  sortBy: string,
  createDate: string,
  id: number
): Fetch1Payload {
  if (sortBy === "CreateDate") {
    return {
      mode: "since",
      createDate: createDate,
      id: id,
      sortRule: "CreateDate",
      reverse: false
    };
  } else {
    return {
      mode: "before",
      createDate: createDate,
      id: id,
      sortRule: "CreateDateDescending",
      reverse: false
    };
  }
}

export function getLeftButtonPayload(
  sortBy: string,
  createDate: string,
  id: number
): Fetch1Payload {
  if (sortBy === "CreateDate") {
    return {
      mode: "before",
      createDate: createDate,
      id: id,
      sortRule: "CreateDateDescending",
      reverse: true
    };
  } else {
    return {
      mode: "since",
      createDate: createDate,
      id: id,
      sortRule: "CreateDate",
      reverse: true
    };
  }
}

export function updateFetch1Payload(
  responseItems: EntryWithVideosAndVisibility[],
  oldSortingCondition: string,
  newSortingCondition: string,
  timestampFirst: string,
  timestampLast: string,
  additionMode: string,
  direction: string
): Fetch1Payload {
  const minId =
    responseItems.length > 0 ? Math.min(...responseItems.map(video => video.song.id)) : 10000000;
  const maxId =
    responseItems.length > 0 ? Math.max(...responseItems.map(video => video.song.id)) : 0;
  if (oldSortingCondition === "CreateDate") {
    if (direction == "right") {
      return getRightButtonPayload(newSortingCondition, timestampLast, maxId);
    } else {
      return getLeftButtonPayload(oldSortingCondition, timestampFirst, minId);
    }
  } else {
    if (direction == "right") {
      return getRightButtonPayload(newSortingCondition, timestampLast, maxId);
    } else {
      return getLeftButtonPayload(
        oldSortingCondition,
        oldSortingCondition === "CreateDate" ? timestampFirst : timestampLast,
        additionMode === "before" ? minId : maxId
      );
    }
  }
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
  reverse: boolean;
}

export interface EntryWithVideosAndVisibility {
  thumbnailsOk: NicoVideoWithMappedTags[];
  thumbnailsErr: NicoVideoWithError[];
  song: SongForApiContractSimplified;
  visible: boolean;
  toAssign: boolean;
  tagsToAssign: MinimalTag[];
}
