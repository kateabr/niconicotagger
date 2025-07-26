import { QueryConsoleApiType } from "@/backend/dto/enumeration";

export interface QueryConsoleRequest {
  apiType: QueryConsoleApiType;
  query: string;
  clientType: string;
}
