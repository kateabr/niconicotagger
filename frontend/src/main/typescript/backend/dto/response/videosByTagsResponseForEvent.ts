import {
  AvailableNndVideoWithAdditionalData,
  SongEntryWithReleaseEventInfo
} from "@/backend/dto/higherLevelStruct";
import { PublisherInfo, UpdateErrorReport } from "@/backend/dto/lowerLevelStruct";
import { DispositionRelativelyToDate } from "@/backend/dto/dateDisposition";

export interface VideosByTagsResponseForEvent {
  items: NndVideoWithAssociatedVocaDbEntryForEvent[];
  totalCount: number;
  cleanScope: string;
}

export interface NndVideoWithAssociatedVocaDbEntryForEvent {
  video: AvailableNndVideoWithAdditionalData;
  entry: SongEntryWithReleaseEventInfo | null;
  publisher: PublisherInfo | null;
  publishedOn: string;
  disposition: DispositionRelativelyToDate;
  toUpdate: boolean;
  visible: boolean;
  processed: boolean;
  errorReport: UpdateErrorReport | null;
}
