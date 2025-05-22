import { SongTypeStats } from "@/backend/dto/songTypeStats";
import { SongType } from "@/backend/dto/enumeration";
import { ReleaseEvent, UpdateErrorReport } from "@/backend/dto/lowerLevelStruct";
import { DispositionRelativelyToDate } from "@/backend/dto/dateDisposition";

export interface SongsByVocaDbEventTagResponse {
  items: SongEntryByVocaDbTagForEvent[];
  songTypeStats: SongTypeStats;
  totalCount: number;
}

export interface SongEntryByVocaDbTagForEvent {
  id: number;
  name: string;
  type: string;
  artistString: string;
  publishedOn: string;
  events: ReleaseEvent[];
  disposition: DispositionRelativelyToDate;
  checked: boolean;
  toAddEvent: boolean;
  processed: boolean;
  visible: boolean;
  errorReport: UpdateErrorReport | null;
}
