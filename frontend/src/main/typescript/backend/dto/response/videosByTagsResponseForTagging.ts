import { SongTypeStats } from "@/backend/dto/songTypeStats";
import { VocaDbTag } from "@/backend/dto/lowerLevelStruct";
import { NndVideoWithAssociatedVocaDbEntryForTag } from "@/backend/dto/higherLevelStruct";

export interface VideosByTagsResponseForTagging {
  items: NndVideoWithAssociatedVocaDbEntryForTag[];
  totalCount: number;
  cleanScope: string;
  songTypeStats: SongTypeStats;
  tagMappings: VocaDbTag[];
}
