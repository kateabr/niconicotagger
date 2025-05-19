import { AxiosResponse } from "axios";
import { ArtistType, ClientType, SongType } from "@/backend/dto/enumeration";
import { SongTypeStats, SongTypeStatsRecord } from "@/backend/dto/songTypeStats";
import { ErrorData, ReleaseEvent, VocaDbTagSelectable } from "@/backend/dto/lowerLevelStruct";
import {
  UnavailableNndVideo,
  VocaDbSongEntryWithPvs
} from "@/backend/dto/response/songsWithPvsResponse";
import {
  dbBaseUrl,
  localStorageKeyClientType,
  nndOrderOptions,
  videoStatusesToDisable,
  vocaDbOrderOptions
} from "@/constants";

// url generators
export function getBaseUrl(clientType: ClientType): string {
  return dbBaseUrl[clientType];
}

export function getNicoTagUrl(tag: string, scope: string): string {
  if (scope.length > 0) {
    return "https://www.nicovideo.jp/tag/" + tag + " " + scope;
  }
  return "https://www.nicovideo.jp/tag/" + tag;
}

export function getNicoVideoUrl(contentId: string): string {
  return "https://www.nicovideo.jp/watch/" + contentId;
}

export function getNicoEmbedUrl(videoId: string): string {
  return "https://embed.nicovideo.jp/watch/" + videoId + "?noRelatedVideo=1&enablejsapi=0";
}

export function getVocaDBEventUrl(database: ClientType, id: number): string {
  return dbBaseUrl[database] + "/E/" + id;
}

export function getVocaDBSongUrl(database: ClientType, id: number): string {
  return dbBaseUrl[database] + "/S/" + id;
}

export function getVocaDBTagUrl(database: ClientType, id: number): string {
  return dbBaseUrl[database] + "/T/" + id;
}

export function getVocaDBAddSongUrl(database: ClientType, pvLink: string): string {
  return dbBaseUrl[database] + "/Song/Create?pvUrl=https://www.nicovideo.jp/watch/" + pvLink;
}

export function getVocaDBArtistUrl(database: ClientType, artistId: number): string {
  return dbBaseUrl[database] + "/Ar/" + artistId;
}

export function getDeletedVideoUrl(videoId: string): string {
  return "https://www.nicolog.jp/watch/" + videoId;
}

// common predicates
export function pageStateIsValid(pageToJump: number, maxPage: number): boolean {
  return pageToJump > 0 && pageToJump <= maxPage;
}

export function infoLoaded(listOfMediaLength: number, frozenTextValue: string): boolean {
  return listOfMediaLength > 0 && frozenTextValue != "";
}

export function allVideosInvisible(list: { visible: boolean }[]): boolean {
  return list.every(item => !item.visible);
}

export function shouldDisableByStatus(pv: UnavailableNndVideo): boolean {
  return videoStatusesToDisable.includes(pv.error);
}

// common getters
export function getClientType(): ClientType {
  const clientType = localStorage.getItem(localStorageKeyClientType);
  if (clientType == null || ClientType[clientType as keyof typeof ClientType] == undefined) {
    return ClientType.UNKNOWN;
  }
  return ClientType[clientType as keyof typeof ClientType];
}

export function getErrorData(err: { response: AxiosResponse }): ErrorData {
  if (typeof err.response.data == "object") {
    if ("violations" in err.response.data) {
      return {
        message: (Object.entries(err.response.data).filter(
          entry => entry[0] == "violations"
        )[0][1] as Array<any>)
          .map(violationTuple => `${violationTuple["field"]} ${violationTuple["message"]}`)
          .join("<br>"),
        statusText: "Constraint violation in request"
      };
    } else {
      let alertMessage: string;
      if ("detail" in err.response.data) {
        alertMessage = Object.entries(err.response.data).filter(
          entry => entry[0] == "detail"
        )[0][1] as string;
      } else {
        alertMessage = err.response.data;
      }
      return {
        statusText: err.response.statusText,
        message: alertMessage
      };
    }
  } else {
    return {
      statusText: err.response.statusText,
      message: err.response.data
    };
  }
}

export function getMaxResultsForDisplay(maxResults: number): string {
  return "Results per page: " + maxResults;
}

export function getOrderingConditionForDisplay(orderingCondition: string): string {
  return "Arrange by: " + vocaDbOrderOptions[orderingCondition];
}

export function getSortingConditionForDisplayNico(orderingCondition: string): string {
  return "Arrange by: " + nndOrderOptions[orderingCondition];
}

export function getUniqueElementId(prefix: string, key: string): string {
  return prefix + key;
}

export function getTagVariant(tag: VocaDbTagSelectable, tagsIdsToAssign: number[]): string {
  if (tag.selected) {
    return "success";
  } else if (tagsIdsToAssign.find(tagId => tagId == tag.tag.id) != undefined) {
    return "warning";
  } else {
    return "outline-success";
  }
}

export function getShortenedSongType(typeString: SongType): string {
  if (SongType[typeString] == SongType.Unspecified) {
    return "?";
  } else if (SongType[typeString] == SongType.MusicPV) {
    return "PV";
  } else {
    return typeString[0];
  }
}

export function getShortenedArtistType(typeString: ArtistType): string {
  if (typeString == "Unknown") {
    return "?";
  } else if (typeString == "SynthesizerV") {
    return "SV";
  } else if (typeString == "VoiSona") {
    return "VS";
  } else if (typeString == "NewType") {
    return "NT";
  } else if (typeString == "Voiceroid") {
    return "VR";
  } else if (typeString == "VOICEVOX") {
    return "VV";
  } else if (typeString == "AIVOICE") {
    return "AIV";
  } else if (typeString == "ACEVirtualSinger") {
    return "ACE";
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

export function mapSongTypeStats(
  songTypeStats: SongTypeStats,
  existingStats: SongTypeStatsRecord[]
): SongTypeStatsRecord[] {
  return Object.entries(songTypeStats)
    .map(key => {
      return {
        type: key[0],
        count: key[1] as number,
        show:
          existingStats.length > 0
            ? existingStats.filter(statsItem => statsItem.type == key[0])[0].show
            : true
      };
    })
    .sort((statsItem1, statsItem2) => {
      return SongType[statsItem1.type].valueOf() - SongType[statsItem2.type].valueOf();
    });
}

export function formatDateString(date: string | null, endDate: string | null): string | null {
  let result = "";
  if (date != null) {
    result += new Date(date).toLocaleDateString();
  }
  if (endDate != null) {
    result += " - " + new Date(endDate).toLocaleDateString();
  }
  return result.length > 0 ? result : null;
}

// common interface methods
export function getSongTypeColorForDisplay(songType: SongType): string {
  const castType = SongType[songType];
  if (castType == SongType.Original || castType == SongType.Remaster) {
    return "primary";
  } else if (
    castType == SongType.Remix ||
    castType == SongType.Cover ||
    castType == SongType.Mashup ||
    castType == SongType.Other
  ) {
    return "secondary";
  } else if (castType == SongType.Instrumental) {
    return "dark";
  } else if (castType == SongType.MusicPV || castType == SongType.DramaPV) {
    return "success";
  } else {
    return "warning";
  }
}

export function getArtistTypeColorForDisplay(typeString: ArtistType): string {
  if (typeString == "Vocaloid") {
    return "primary";
  } else if (typeString == "UTAU") {
    return "danger";
  } else if (typeString == "CeVIO") {
    return "success";
  } else if (typeString == "OtherVoiceSynthesizer") {
    return "dark";
  } else {
    return "secondary";
  }
}

export function getEventColorVariant(event: ReleaseEvent, eventId: number): string {
  if (event.id == eventId) {
    return "success";
  } else {
    return "warning";
  }
}

// other util methods

export function toggleTagAssignment(tag: VocaDbTagSelectable, entry: VocaDbSongEntryWithPvs): void {
  const assign = entry.tagIdsToAssign.filter(tagId => tagId == tag.tag.id).length > 0;
  if (assign) {
    entry.tagIdsToAssign = entry.tagIdsToAssign.filter(tagId => tagId != tag.tag.id);
  } else {
    entry.tagIdsToAssign.push(tag.tag.id);
  }
  entry.toUpdate = entry.tagIdsToAssign.length > 0;
}

export function getTagIconForTagAssignmentButton(
  tag: VocaDbTagSelectable,
  tagIdsToAssign: number[]
): string[] {
  if (tag.selected) {
    return ["fas", "fa-check"];
  } else if (tagIdsToAssign.find(tagId => tagId == tag.tag.id) != undefined) {
    return ["fas", "fa-minus"];
  } else {
    return ["fas", "fa-plus"];
  }
}
