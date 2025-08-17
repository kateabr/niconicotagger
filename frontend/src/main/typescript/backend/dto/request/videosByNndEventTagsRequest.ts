import { NndSortOrder } from "@/backend/dto/enumeration";
import { EventDateBounds } from "@/backend/dto/lowerLevelStruct";

export interface VideosByNndEventTagsRequest {
  tags: string[];
  scope: string;
  startOffset: number;
  maxResults: number;
  orderBy: NndSortOrder;
  dates: EventDateBounds;
  eventId: number;
  clientType: string;
}
