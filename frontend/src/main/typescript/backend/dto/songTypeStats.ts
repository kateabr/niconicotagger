import { SongType } from "@/backend/dto/enumeration";

export type SongTypeStats = { [key in SongType]: SongTypeStatsRecord };

export interface SongTypeStatsRecord {
  type: string;
  count: number;
  show: boolean;
}
