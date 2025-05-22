import { QueryConsoleApiType } from "@/backend/dto/enumeration";
import { VocaDbTag } from "@/backend/dto/lowerLevelStruct";

export interface MassDeleteTagUsagesRequest {
  subRequests: DeleteTagUsagesRequest[];
  clientType: string;
}

export interface DeleteTagUsagesRequest {
  apiType: QueryConsoleApiType;
  entryId: number;
  tags: VocaDbTag[];
}
