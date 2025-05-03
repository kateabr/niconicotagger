import { NndSortOrder } from "@/backend/dto/enumeration";

export interface VideosByNndTagsRequest {
  tags: string[];
  scope: string;
  startOffset: number;
  maxResults: number;
  orderBy: NndSortOrder;
  clientType: string;
}
