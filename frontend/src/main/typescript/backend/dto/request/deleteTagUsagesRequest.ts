import { QueryConsoleApiType } from "@/backend/dto/enumeration";

export interface MassDeleteTagUsagesRequest {
  subRequests: DeleteTagUsagesRequest[];
  clientType: string;
}

export interface DeleteTagUsagesRequest {
  apiType: QueryConsoleApiType;
  entryId: number;
  tagIds: number[];
}
