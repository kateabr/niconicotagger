import { ReleaseEventCategory, SongType } from "@/backend/dto/enumeration";
import { VocaDbTagSelectable } from "@/backend/dto/lowerLevelStruct";
import {
  NndTagData,
  PublisherInfo,
  ReleaseEventWithSeriesId,
  UpdateErrorReport
} from "@/backend/dto/lowerLevelStruct";

export interface ReleaseEventDataWithNndTags {
  id: number;
  dateString: string | null;
  date: string | null;
  endDate: string | null;
  name: string;
  category: ReleaseEventCategory;
  nndTags: string[];
  valid: boolean;
  seriesId: number | null;
}

export interface ReleaseEventData {
  id: number;
  dateString: string | null;
  date: string | null;
  endDate: string | null;
  name: string;
  category: ReleaseEventCategory;
  valid: boolean;
}

export interface SongEntryWithReleaseEventInfo {
  id: number;
  name: string;
  type: string;
  artistString: string;
  events: ReleaseEventWithSeriesId[];
}

export interface SongEntryWithTagAssignmentInfo {
  id: number;
  name: string;
  type: string;
  artistString: string;
  publishedOn: string;
  mappedTags: VocaDbTagSelectable[];
}

export interface AvailableNndVideoWithAdditionalData {
  id: string;
  title: string;
  description: string | null;
  tags: NndTagData[];
  length: string;
  visible: boolean;
}

export interface NndVideoWithAssociatedVocaDbEntryForTag {
  video: AvailableNndVideoWithAdditionalData;
  entry: SongEntryWithTagAssignmentInfo | null;
  publisher: PublisherInfo | null;
  selected: boolean;
  visible: boolean;
  processed: boolean;
  errorReport: UpdateErrorReport | null;
}
