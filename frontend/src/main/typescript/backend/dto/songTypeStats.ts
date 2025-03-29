import { SongType } from "@/backend/dto/enumeration";

export type SongTypeStats = { [key: SongType]: SongTypeStatsRecord };

export interface SongTypeStatsRecord {
  type: SongType;
  count: number;
  show: boolean;
}
