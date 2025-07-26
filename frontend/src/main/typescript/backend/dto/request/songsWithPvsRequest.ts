import { DbSortOrder } from "@/backend/dto/enumeration";

export interface SongsWithPvsRequest {
  startOffset: number;
  maxResults: number;
  orderBy: DbSortOrder;
  clientType: string;
}
