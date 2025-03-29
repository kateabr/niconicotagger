import { ClientType, DbSortOrder } from "@/backend/dto/enumeration";

export interface SongsWithPvsRequest {
  startOffset: number;
  maxResults: number;
  orderBy: DbSortOrder;
  clientType: ClientType;
}
