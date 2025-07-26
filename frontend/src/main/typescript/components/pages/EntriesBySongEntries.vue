<template>
  <b-row>
    <error-message
      :alert-message="alertMessage"
      :this-mode="thisMode"
      :alert-status-text="alertStatusText"
    />
    <b-row
      class="pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-col class="my-auto">
        <b-dropdown
          :disabled="defaultDisableCondition() || sessionLocked"
          block
          :text="getMaxResultsForDisplay()"
          class="my-auto"
          variant="primary"
          menu-class="w-100"
        >
          <span
            v-for="maxResultOption in maxResultsOptions"
            :key="maxResultOption"
          >
            <b-dropdown-item
              :disabled="maxResults === maxResultOption"
              @click="setMaxResults(maxResultOption)"
              >{{ maxResultOption }}
            </b-dropdown-item>
          </span>
        </b-dropdown>
      </b-col>
      <b-col class="my-auto">
        <b-dropdown
          block
          :disabled="defaultDisableCondition() || sessionLocked"
          :text="getOrderingConditionForDisplay()"
          variant="primary"
          menu-class="w-100"
        >
          <b-dropdown-item
            v-for="(key, value) in orderOptions"
            :key="key"
            :disabled="orderingCondition === value"
            @click="setOrderingCondition(value)"
          >
            {{ orderOptions[value] }}
          </b-dropdown-item>
        </b-dropdown>
      </b-col>
      <b-col v-if="songsInfoLoaded()" class="my-auto">
        <template>
          <b-input-group
            inline
            :state="pageStateIsValid"
            invalid-feedback="Wrong page number"
          >
            <template #prepend>
              <b-input-group-text
                class="justify-content-center"
                style="width: 82px"
                >Page:
              </b-input-group-text>
            </template>
            <template>
              <b-form-input
                id="page-jump-form"
                v-model.number="pageToJump"
                type="number"
                :disabled="defaultDisableCondition()"
                aria-describedby="input-live-help input-live-feedback"
                :state="pageStateIsValid()"
                @keydown.enter.native="
                  pageStateIsValid()
                    ? fetch((pageToJump - 1) * maxResults, pageToJump)
                    : null
                "
              >
              </b-form-input>
            </template>
            <template #append>
              <b-button
                style="width: 82px"
                :variant="pageStateIsValid() ? 'success' : 'danger'"
                :disabled="defaultDisableCondition() || !pageStateIsValid()"
                @click="loadPage(pageToJump)"
              >
                <span v-if="fetching"><b-spinner small /></span>
                <span v-else-if="pageToJump === page">Refresh</span>
                <span v-else>Jump</span>
              </b-button>
            </template>
          </b-input-group>
        </template>
      </b-col>
      <b-col v-else class="m-auto">
        <b-button
          v-if="!sessionLocked && songs.length === 0"
          variant="primary"
          block
          :disabled="defaultDisableCondition()"
          @click="fetch(0, 1)"
          ><span v-if="fetching"><b-spinner small /></span>
          <span v-else>Load</span>
        </b-button>
        <b-button-group v-else class="btn-group-justified w-100">
          <b-button
            block
            variant="success"
            :disabled="defaultDisableCondition()"
            @click="fetch(startOffset, ~~(startOffset / maxResults + 1))"
            ><span v-if="fetching"><b-spinner small /></span>
            <span v-else
              >Restore page {{ ~~(startOffset / maxResults + 1) }}</span
            >
          </b-button>
          <b-button
            style="width: 82px"
            variant="danger"
            :disabled="defaultDisableCondition()"
            @click="unlockSession"
          >
            Clear
          </b-button>
        </b-button-group>
      </b-col>
      <b-col class="my-auto">
        <b-button
          variant="primary"
          block
          :pressed.sync="showEntriesWithErrors"
          :disabled="defaultDisableCondition()"
          @click="filterSongs()"
          >Entries with errors
        </b-button>
      </b-col>
      <b-col class="my-auto">
        <b-button
          variant="primary"
          block
          :pressed.sync="showEntriesWithNoTags"
          :disabled="defaultDisableCondition()"
          @click="filterSongs()"
          >Entries with no tags to add
        </b-button>
      </b-col>
    </b-row>
    <b-row v-if="songsInfoLoaded()" class="col-12">
      <b-col class="my-auto">
        <div class="text-center pt-sm-3">
          <b-button-group>
            <b-button
              v-for="(songType, songTypeKey) in songTypeStats"
              :key="songTypeKey"
              class="pl-4 pr-4"
              :disabled="defaultDisableCondition()"
              :variant="
                (songType.show ? '' : 'outline-') +
                getSongTypeColorForDisplay(songType.type)
              "
              @click="
                songType.show = !songType.show;
                filterSongs();
              "
              >{{ songType.type }} ({{ songType.count }})
            </b-button>
          </b-button-group>
        </div>
      </b-col>
    </b-row>
    <b-row v-if="songsInfoLoaded()" class="col-12">
      <template>
        <div class="overflow-auto m-auto mt-lg-3">
          <b-pagination
            v-model="page"
            align="center"
            :total-rows="totalSongCount"
            :per-page="maxResults"
            use-router
            first-number
            last-number
            limit="10"
            :disabled="defaultDisableCondition()"
            @input="loadPage(page)"
          />
        </div>
      </template>
    </b-row>
    <b-table-simple
      v-if="songsInfoLoaded() && isActiveMode()"
      hover
      class="mt-1 col-lg-12"
    >
      <b-thead>
        <b-th>
          <b-form-checkbox class="invisible" size="lg"></b-form-checkbox>
        </b-th>
        <b-th class="col-3 align-middle">Entry</b-th>
        <b-th class="col-9 align-middle">Videos</b-th>
      </b-thead>
      <b-tbody v-if="songs.filter(s => s.visible).length > 0">
        <tr v-for="song in songs.filter(s => s.visible)" :key="song.entry.id">
          <td>
            <div v-if="song.availablePvs.length > 0">
              <b-form-checkbox
                v-if="song.tagIdsToAssign.length > 0"
                v-model="song.toUpdate"
                size="lg"
                :disabled="defaultDisableCondition()"
              ></b-form-checkbox>
            </div>
          </td>
          <td>
            <b-link target="_blank" :href="getVocaDBEntryUrl(song.entry.id)"
              >{{ song.entry.name }}
              <b-badge
                class="badge text-center ml-1"
                :variant="getSongTypeColorForDisplay(song.entry.type)"
              >
                {{ getShortenedSongType(song.entry.type) }}
              </b-badge>
            </b-link>
            <b-badge
              v-if="song.entry.publishedOn == null"
              variant="danger"
              class="ml-2"
            >
              <font-awesome-icon class="mr-sm-1" icon="fas fa-calendar" />NO
              PUBLISH DATE</b-badge
            >
            <div class="text-muted">
              {{ song.entry.artistString }}
            </div>
          </td>
          <td>
            <b-row v-for="pv in song.availablePvs" :key="pv.video.id">
              <b-col class="col-8">
                <b-button
                  :disabled="defaultDisableCondition()"
                  size="sm"
                  variant="primary-outline"
                  class="mr-2"
                  @click="pv.visible = !pv.visible"
                >
                  <font-awesome-icon icon="fas fa-play" />
                </b-button>
                <b-link target="_blank" :href="getNicoVideoUrl(pv.video.id)">{{
                  pv.video.title
                }}</b-link>
                <div>
                  <b-badge
                    v-for="(nicoTag, key) in pv.video.tags"
                    :key="key"
                    v-clipboard:copy="nicoTag.name"
                    class="m-sm-1"
                    href="#"
                    :variant="nicoTag.type"
                    >{{ nicoTag.name }}
                    <font-awesome-icon
                      v-if="nicoTag.locked"
                      icon="fas fa-lock"
                      class="ml-1"
                    />
                  </b-badge>
                </div>
                <b-collapse
                  :visible="pv.visible && !fetching"
                  class="mt-2 collapsed"
                >
                  <nico-embed
                    v-if="pv.visible && !fetching"
                    :content-id="pv.video.id"
                  />
                </b-collapse>
                <nico-description
                  :content-id="pv.video.id"
                  :description="pv.video.description"
                  :publisher="null"
                />
              </b-col>
              <b-col>
                <span v-for="tag in pv.suggestedTags" :key="tag.tag.id">
                  <b-button
                    size="sm"
                    class="m-1"
                    :disabled="tag.selected || defaultDisableCondition()"
                    :variant="getTagVariant(tag, song.tagIdsToAssign)"
                    @click="toggleTagAssignment(tag, song)"
                  >
                    <font-awesome-icon
                      :icon="getTagIcon(tag, song.tagIdsToAssign)"
                      class="sm mr-sm-1"
                    />
                    {{ tag.tag.name }}
                  </b-button>
                </span>
                <div v-if="pv.suggestedTags.length === 0" class="text-muted">
                  No mapped tags available for this video
                </div>
              </b-col>
            </b-row>
            <b-row v-for="pv in song.unavailablePvs" :key="pv.id">
              <b-col>
                <b-link :href="getDeletedVideoUrl(pv.id)" target="_blank"
                  >{{ pv.title }}
                  <span
                    ><b-badge variant="danger" size="sm" class="ml-1">{{
                      pv.error
                    }}</b-badge></span
                  ></b-link
                >
                <div>
                  <span
                    ><b-badge
                      v-if="disableByStatus(pv) && pv.toDisable"
                      variant="warning"
                      class="m-1"
                    >
                      Needs to be disabled</b-badge
                    >
                    <b-badge
                      v-else-if="disableByStatus(pv) && !pv.toDisable"
                      variant="success"
                      class="m-1"
                    >
                      Disabled</b-badge
                    >
                    <b-badge
                      v-else-if="pv.error === 'NO DATA'"
                      variant="warning"
                      class="m-1"
                    >
                      Service down?
                    </b-badge>
                    <b-badge v-else variant="warning" class="m-1">
                      Unknown error</b-badge
                    >
                  </span>
                </div>
              </b-col>
              <b-col v-if="disableByStatus(pv) && pv.toDisable" cols="4">
                <b-button size="sm" class="m-1" disabled variant="secondary">
                  <font-awesome-icon
                    icon="fa-solid fa-eye-slash"
                    class="sm mr-sm-1"
                  />
                  Disable
                </b-button>
              </b-col>
            </b-row>
            <entry-error-report :error-report="song.errorReport" />
          </td>
        </tr>
      </b-tbody>
      <b-tbody v-else>
        <b-tr>
          <b-td colspan="4" class="text-center text-muted">
            <small>No items to display</small>
          </b-td>
        </b-tr>
      </b-tbody>
      <b-tfoot>
        <b-th></b-th>
        <b-th class="col-3 align-middle">Entry</b-th>
        <b-th class="col-9 align-middle">Videos</b-th>
      </b-tfoot>
    </b-table-simple>
    <b-row
      v-if="songsInfoLoaded()"
      class="mt-lg-1 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-col class="col-lg-3 m-auto">
        <b-button
          block
          variant="primary"
          :disabled="countChecked() === 0 || assigning || fetching"
          @click="assign"
        >
          <div v-if="assigning">
            <b-spinner small class="mr-1"></b-spinner>
            Assigning...
          </div>
          <div v-else>Batch assign ({{ countChecked() }} selected)</div>
        </b-button>
      </b-col>
    </b-row>

    <b-row v-if="songsInfoLoaded()" class="col-12">
      <template>
        <div class="overflow-auto m-auto my-lg-3">
          <b-pagination
            v-model="page"
            align="center"
            :total-rows="totalSongCount"
            :per-page="maxResults"
            use-router
            first-number
            last-number
            limit="10"
            :disabled="defaultDisableCondition()"
            @input="loadPage(page)"
          />
        </div>
      </template>
    </b-row>
  </b-row>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import {
  shouldDisableByStatus,
  getErrorData,
  getClientType,
  getDeletedVideoUrl,
  getMaxResultsForDisplay,
  getNicoVideoUrl,
  getOrderingConditionForDisplay,
  getShortenedSongType,
  getSongTypeColorForDisplay,
  getTagIconForTagAssignmentButton,
  getTagVariant,
  getUniqueElementId,
  getVocaDBSongUrl,
  mapSongTypeStats,
  pageStateIsValid,
  toggleTagAssignment
} from "@/utils";
import { api } from "@/backend";
import NicoEmbed from "@/components/NicoEmbed.vue";
import ErrorMessage from "@/components/ErrorMessage.vue";
import { AxiosError, AxiosResponse } from "axios";
import NicoDescription from "@/components/NicoDescription.vue";
import EntryErrorReport from "@/components/EntryErrorReport.vue";
import { SongType, DbSortOrder } from "@/backend/dto/enumeration";
import { SongTypeStatsRecord } from "@/backend/dto/songTypeStats";
import { VocaDbTagSelectable } from "@/backend/dto/lowerLevelStruct";
import { VocaDbSongEntryWithPvs } from "@/backend/dto/response/songsWithPvsResponse";
import {
  localStorageKeyMaxResults,
  localStorageKeyStartOffset,
  localStorageKeyDbOrderBy,
  maxResultsOptions,
  vocaDbOrderOptions
} from "@/constants";

@Component({
  methods: { disableByStatus: shouldDisableByStatus },
  components: {
    EntryErrorReport,
    NicoDescription,
    NicoEmbed,
    ErrorMessage
  }
})
export default class extends Vue {
  @Prop()
  private readonly mode!: string;

  @Prop()
  private readonly thisMode!: string;

  private readonly maxResultsOptions: number[] = maxResultsOptions;
  private readonly orderOptions = vocaDbOrderOptions;

  // main variables
  private songs: VocaDbSongEntryWithPvs[] = [];
  private songTypeStats: SongTypeStatsRecord[] = [];

  // api variables
  private maxResults: number = 10;
  private orderingCondition: DbSortOrder = "AdditionDate";
  private pageToJump: number = 0;
  private startOffset: number = 0;
  private clientType: string = getClientType();
  private chunkSize = 10;

  // interface variables
  private fetching: boolean = false;
  private assigning: boolean = false;
  private totalSongCount: number = 0;
  private maxPage: number = 0;
  private entriesWithNoPvsLeft: number = 0;
  private page: number = 1;
  private showEntriesWithNoTags: boolean = false;
  private showEntriesWithErrors: boolean = true;
  private sessionLocked: boolean = false;

  // error handling
  private alertMessage: string = "";
  private alertStatusText = "";

  // interface methods
  private isActiveMode(): boolean {
    return this.mode == this.thisMode;
  }

  private setMaxResults(maxResults: number): void {
    this.maxResults = maxResults;
    localStorage.setItem(localStorageKeyMaxResults, this.maxResults.toString());
  }

  private setOrderingCondition(value: DbSortOrder): void {
    this.orderingCondition = value;
    localStorage.setItem(
      localStorageKeyDbOrderBy,
      this.orderingCondition.toString()
    );
  }

  private getHiddenTypes(): number {
    return this.songTypeStats.filter(statsItem => !statsItem.show).length;
  }

  private countChecked(): number {
    return this.songs.filter(video => video.toUpdate).length;
  }

  // row filtering
  private hiddenTypeFlag(entry: VocaDbSongEntryWithPvs): boolean {
    return (
      this.getHiddenTypes() == 0 ||
      !this.songTypeStats
        .filter(statsItem => !statsItem.show)
        .map(statsItem => statsItem.type)
        .includes(entry.entry.type)
    );
  }

  private hasTagsToAssign(entry: VocaDbSongEntryWithPvs): boolean {
    let assignable_mapped_tags_cnt = 0;
    for (const pv of entry.availablePvs) {
      assignable_mapped_tags_cnt += pv.suggestedTags.filter(
        tag => !tag.selected
      ).length;
    }
    return assignable_mapped_tags_cnt > 0;
  }

  private hideEntriesWithNoTagsFlag(entry: VocaDbSongEntryWithPvs): boolean {
    return this.showEntriesWithNoTags || this.hasTagsToAssign(entry);
  }

  private showEntriesWithErrorsFlag(entry: VocaDbSongEntryWithPvs): boolean {
    return (
      this.showEntriesWithErrors &&
      (entry.unavailablePvs.length > 0 ||
        entry.entry.publishedOn == null ||
        entry.errorReport != null)
    );
  }

  private filterSongs(): void {
    for (const song of this.songs) {
      song.visible =
        song.errorReport != null ||
        (this.hiddenTypeFlag(song) && this.hideEntriesWithNoTagsFlag(song)) ||
        this.showEntriesWithErrorsFlag(song);
    }
  }

  // proxy methods
  private getMaxResultsForDisplay(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }
  private getOrderingConditionForDisplay(): string {
    return getOrderingConditionForDisplay(this.orderingCondition);
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.assigning;
  }

  private songsInfoLoaded(): boolean {
    return this.songs.length > 0 && this.totalSongCount > 0;
  }

  private pageStateIsValid(): boolean {
    return pageStateIsValid(this.pageToJump, this.maxPage);
  }

  private getSongTypeColorForDisplay(typeString: string): string {
    return getSongTypeColorForDisplay(SongType[typeString]);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBSongUrl(this.clientType, id);
  }

  private getDeletedVideoUrl(videoId: string): string {
    return getDeletedVideoUrl(videoId);
  }

  private getNicoVideoUrl(contentId: string): string {
    return getNicoVideoUrl(contentId);
  }

  private getShortenedSongType(songType: string): string {
    return getShortenedSongType(SongType[songType]);
  }

  private getTagVariant(
    tag: VocaDbTagSelectable,
    tagIdsToAssign: number[]
  ): string {
    return getTagVariant(tag, tagIdsToAssign);
  }

  private getTagIcon(
    tag: VocaDbTagSelectable,
    tagIdsToAssign: number[]
  ): string[] {
    return getTagIconForTagAssignmentButton(tag, tagIdsToAssign);
  }

  private toggleTagAssignment(
    tag: VocaDbTagSelectable,
    video: VocaDbSongEntryWithPvs
  ): void {
    toggleTagAssignment(tag, video);
  }

  private updateUrl(): void {
    this.$router
      .push({
        name: "tags-mode",
        params: { browseMode: this.thisMode }
      })
      .catch(err => {
        return false;
      });
  }

  // api methods
  async fetch(newStartOffset: number, newPage: number): Promise<void> {
    this.updateUrl();
    this.fetching = true;
    try {
      let response = await api.getVocaDbSongEntriesForTagging({
        startOffset: newStartOffset - this.entriesWithNoPvsLeft,
        maxResults: this.maxResults,
        orderBy: this.orderingCondition,
        clientType: this.clientType
      });
      this.totalSongCount = response.totalCount;
      this.songs = response.items.map(entry => {
        const unavailablePvs = entry.unavailablePvs.map(unavailablePv => {
          return {
            id: unavailablePv.id,
            title: unavailablePv.title,
            error: unavailablePv.error,
            toDisable: shouldDisableByStatus(unavailablePv)
          };
        });
        return {
          entry: entry.entry,
          availablePvs: entry.availablePvs.map(pv => {
            return {
              video: pv.video,
              suggestedTags: pv.suggestedTags,
              visible: false
            };
          }),
          unavailablePvs: unavailablePvs,
          toUpdate: unavailablePvs.filter(pv => pv.toDisable).length > 0,
          visible: true,
          tagIdsToAssign: [],
          errorReport: null
        };
      });
      this.filterSongs();
      this.startOffset = newStartOffset - this.entriesWithNoPvsLeft;
      this.songTypeStats = mapSongTypeStats(
        response.songTypeStats,
        this.songTypeStats
      );
      this.entriesWithNoPvsLeft = 0;
      this.pageToJump = newPage;
      this.maxPage = Math.ceil(this.totalSongCount / this.maxResults);
      this.page = newPage;
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      localStorage.setItem(
        localStorageKeyMaxResults,
        this.maxResults.toString()
      );
      localStorage.setItem(
        localStorageKeyStartOffset,
        this.startOffset.toString()
      );
      localStorage.setItem(localStorageKeyDbOrderBy, this.orderingCondition);
      this.fetching = false;
    }
  }

  private async assign(): Promise<void> {
    this.assigning = true;
    try {
      const songsToUpdate = this.songs.filter(song => song.toUpdate);
      let startPosition = 0;
      let currentChunk = songsToUpdate.slice(
        startPosition,
        startPosition + this.chunkSize
      );
      while (currentChunk.length > 0) {
        await this.update(currentChunk);
        startPosition += this.chunkSize;
        currentChunk = songsToUpdate.slice(startPosition, startPosition + 10);
      }
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      this.assigning = false;
    }
  }

  private async update(songsToUpdate: VocaDbSongEntryWithPvs[]): Promise<void> {
    const errors = await api.updateSongTagsAndPvs({
      subRequests: songsToUpdate.map(song => {
        return {
          songId: song.entry.id,
          pvId: null,
          tags: song.tagIdsToAssign,
          nndPvsToDisable: song.unavailablePvs
            .filter(unavailablePv => unavailablePv.toDisable)
            .map(unavailablePv => {
              return {
                id: unavailablePv.id,
                reason: unavailablePv.error
              };
            })
        };
      }),
      clientType: this.clientType
    });
    const entriesWithErrors = errors.map(error => error.entryId);
    for (const song of songsToUpdate) {
      if (entriesWithErrors.includes(song.entry.id)) {
        song.errorReport = errors.filter(
          error => error.entryId == song.entry.id
        )[0];
      } else {
        song.toUpdate = false;
        if (
          song.unavailablePvs.some(unavailablePv => unavailablePv.toDisable)
        ) {
          song.unavailablePvs.forEach(
            unavailablePv => (unavailablePv.toDisable = false)
          );
          if (song.availablePvs.length == 0) {
            this.entriesWithNoPvsLeft += 1;
          }
        }
        for (const pv of song.availablePvs) {
          for (const tag of pv.suggestedTags) {
            tag.selected =
              tag.selected || song.tagIdsToAssign.includes(tag.tag.id);
          }
        }
        song.tagIdsToAssign.splice(0, song.tagIdsToAssign.length);
        song.errorReport = null;
      }
    }
  }

  private loadPage(pgNum: number): void {
    this.fetch((pgNum - 1) * this.maxResults, pgNum);
  }

  // error handling
  private processError(response: AxiosResponse | undefined): void {
    const errorData = getErrorData(response);
    this.alertMessage = errorData.message;
    this.alertStatusText = errorData.statusText;
    this.$bvToast.show(getUniqueElementId("error_", this.thisMode.toString()));
  }

  // session
  created(): void {
    let max_results = localStorage.getItem(localStorageKeyMaxResults);
    if (max_results != null) {
      this.maxResults = parseInt(max_results);
    }
    let start_offset = localStorage.getItem(localStorageKeyStartOffset);
    if (start_offset != null) {
      this.startOffset = parseInt(start_offset);
    }
    let order_by = localStorage.getItem(localStorageKeyDbOrderBy);
    if (order_by != null) {
      this.orderingCondition = order_by as DbSortOrder;
    }
    if (max_results != null && start_offset != null && order_by != null) {
      this.sessionLocked = true;
    }
  }

  private unlockSession(): void {
    this.sessionLocked = false;
  }
}
</script>
