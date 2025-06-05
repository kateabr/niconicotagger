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

export function getErrorData(response: AxiosResponse | undefined): ErrorData {
  if (response == undefined) {
    return {
      message: "Unknown error",
      statusText: "Could not handle error"
    };
  }
  if (typeof response.data == "object") {
    if ("violations" in response.data) {
      return {
        message: (Object.entries(response.data).filter(
          entry => entry[0] == "violations"
        )[0][1] as Array<any>)
          .map(violationTuple => `${violationTuple["field"]} ${violationTuple["message"]}`)
          .join("<br>"),
        statusText: "Constraint violation in request"
      };
    } else {
      let alertMessage: string;
      if ("detail" in response.data) {
        alertMessage = Object.entries(response.data).filter(
          entry => entry[0] == "detail"
        )[0][1] as string;
      } else {
        alertMessage = response.data;
      }
      return {
        statusText: response.statusText,
        message: alertMessage
      };
    }
  } else {
    return {
      statusText: response.statusText,
      message: response.data
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

export function getShortenedSongType(type: SongType): string {
  switch (type) {
    case SongType.Unspecified:
      return "?";
    case SongType.MusicPV:
      return "PV";
    default:
      return SongType[type][0];
  }
}

export function getShortenedArtistType(typeString: ArtistType): string {
  switch (typeString) {
    case "Unknown":
      return "?";
    case "SynthesizerV":
      return "SV";
    case "VoiSona":
      return "VS";
    case "NewType":
      return "NT";
    case "Voiceroid":
      return "VR";
    case "VOICEVOX":
      return "VV";
    case "AIVOICE":
      return "AIV";
    case "ACEVirtualSinger":
      return "ACE";
    case "Vocaloid":
    case "UTAU":
    case "CeVIO":
    case "OtherVoiceSynthesizer":
    case "OtherVocalist":
      return typeString[0];
    default:
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
        count: (key[1] as unknown) as number,
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
  switch (songType) {
    case SongType.Original:
    case SongType.Remaster:
      return "primary";
    case SongType.Remix:
    case SongType.Cover:
    case SongType.Mashup:
    case SongType.Other:
      return "secondary";
    case SongType.Instrumental:
      return "dark";
    case SongType.MusicPV:
    case SongType.DramaPV:
      return "success";
    default:
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
