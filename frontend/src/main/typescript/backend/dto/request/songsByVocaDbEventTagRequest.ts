import { DbSortOrder } from "@/backend/dto/enumeration";
import { EventDateBounds } from "@/backend/dto/lowerLevelStruct";

export interface SongsByVocaDbEventTagRequest {
  tagId: number;
  startOffset: number;
  maxResults: number;
  orderBy: DbSortOrder;
  dates: EventDateBounds;
  clientType: string;
}
