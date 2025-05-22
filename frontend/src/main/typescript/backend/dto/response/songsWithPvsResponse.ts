import { SongTypeStats } from "@/backend/dto/songTypeStats";
import { NndTagData, UpdateErrorReport, VocaDbTagSelectable } from "@/backend/dto/lowerLevelStruct";
import { SongType } from "@/backend/dto/enumeration";

export interface SongsWithPvsResponse {
  items: VocaDbSongEntryWithPvs[];
  songTypeStats: SongTypeStats;
  totalCount: number;
}

export interface VocaDbSongEntryWithPvs {
  entry: SongEntry;
  availablePvs: PvWithSuggestedTags[];
  unavailablePvs: UnavailableNndVideo[];
  toUpdate: boolean;
  visible: boolean;
  tagIdsToAssign: number[];
  errorReport: UpdateErrorReport | null;
}

export interface SongEntry {
  id: number;
  name: string;
  type: string;
  artistString: string;
  publishedOn: Date;
}

export interface PvWithSuggestedTags {
  video: AvailableNndVideo;
  suggestedTags: VocaDbTagSelectable[];
  visible: boolean;
}

export interface UnavailableNndVideo {
  id: string;
  title: string;
  error: string;
  toDisable: boolean;
}

export interface AvailableNndVideo {
  id: string;
  title: string;
  description: string | null;
  tags: NndTagData[];
}
