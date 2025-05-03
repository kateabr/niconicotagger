import { NndSortOrder } from "@/backend/dto/enumeration";

export interface VideosByVocaDbTagRequest {
  tag: string;
  scope: string;
  startOffset: number;
  maxResults: number;
  orderBy: NndSortOrder;
  clientType: string;
}
