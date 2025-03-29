import { SongTypeStats } from "@/backend/dto/songTypeStats";

import { NndVideoWithAssociatedVocaDbEntryForTag } from "@/backend/dto/higherLevelStruct";
import { VocaDbTag } from "@/backend/dto/lowerLevelStruct";

export interface VideosByVocaDbTagResponse {
  items: NndVideoWithAssociatedVocaDbEntryForTag[];
  totalCount: number;
  cleanScope: string;
  songTypeStats: SongTypeStats;
  tag: VocaDbTag;
  tagMappings: string[];
}
