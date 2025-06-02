<template>
  <span>
    <error-message
      :alert-status-text="alertStatusText"
      :alert-message="alertMessage"
      :this-mode="thisMode"
    />
    <b-row>
      <b-col></b-col>
      <b-col cols="5" class="m-auto">
        <b-input-group inline class="mt-lg-3">
          <template #prepend>
            <b-button
              v-b-toggle="'scope-collapse-' + thisMode"
              variant="primary"
              style="width: 82px"
              :disabled="defaultDisableCondition()"
              ><font-awesome-icon
                class="mr-sm-1"
                icon="fas fa-angle-down"
              />More</b-button
            >
          </template>
          <b-form-input
            id="tag-form"
            v-model="tagName"
            :disabled="defaultDisableCondition()"
            placeholder="VocaDB tag"
            @keydown.enter.native="loadInitialPage"
          >
          </b-form-input>
          <template #append>
            <b-button
              v-if="!fetching"
              variant="primary"
              style="width: 82px"
              :disabled="defaultDisableCondition()"
              @click="loadInitialPage()"
              >Load</b-button
            >
            <b-button v-else variant="primary" style="width: 82px" disabled
              ><b-spinner small></b-spinner
            ></b-button>
          </template>
        </b-input-group>
        <b-collapse
          :id="'scope-collapse-' + thisMode"
          v-model="showCollapse"
          class="mt-2"
        >
          <b-row>
            <b-col>
              <b-input-group inline>
                <b-form-input
                  id="scope-tag-form"
                  v-model="scopeTagString"
                  placeholder="Specify tag scope (NND)"
                  :disabled="defaultDisableCondition()"
                  @keydown.enter.native="loadInitialPage"
                >
                </b-form-input>
                <template #prepend>
                  <b-button
                    variant="secondary"
                    style="width: 82px"
                    @click="setDefaultScopeTagString"
                  >
                    <font-awesome-icon icon="fa-solid fa-paste" />
                  </b-button>
                </template>
                <template #append>
                  <b-button
                    variant="danger"
                    style="width: 82px"
                    :disabled="scopeTagString === ''"
                    @click="scopeTagString = ''"
                    >Clear</b-button
                  >
                </template>
              </b-input-group>
            </b-col>
          </b-row>
          <b-row v-if="tagInfoLoaded() && isActiveMode()" class="mt-2">
            <b-col>
              <b-dropdown
                block
                :disabled="defaultDisableCondition()"
                :text="getSortingCondition()"
                variant="primary"
                menu-class="w-100"
              >
                <b-dropdown-item
                  v-for="(key, item) in orderOptionsNico"
                  :key="key"
                  :disabled="sortingCondition === item"
                  @click="setSortingCondition(item)"
                >
                  {{ orderOptionsNico[item] }}
                </b-dropdown-item>
              </b-dropdown>
            </b-col>
            <b-col>
              <template>
                <b-input-group
                  inline
                  :state="pageStateIsValid()"
                  invalid-feedback="Wrong page number"
                >
                  <template #prepend>
                    <b-input-group-text
                      class="justify-content-center"
                      style="width: 82px"
                      >Page:</b-input-group-text
                    >
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
                        pageStateIsValid() ? loadPage(pageToJump) : null
                      "
                    >
                    </b-form-input>
                  </template>
                  <template #append>
                    <b-button
                      style="width: 82px"
                      :variant="pageStateIsValid() ? 'success' : 'danger'"
                      :disabled="
                        defaultDisableCondition() || !pageStateIsValid()
                      "
                      @click="loadPage(pageToJump)"
                      ><span v-if="pageToJump === page">Refresh</span
                      ><span v-else>Jump</span></b-button
                    >
                  </template>
                </b-input-group>
              </template>
            </b-col>
          </b-row>
          <b-row v-if="tagInfoLoaded() && isActiveMode()" class="mt-2">
            <b-col>
              <b-form-checkbox
                v-model="showVideosWithUploaderEntry"
                @change="filterVideos()"
              >
                Force show videos whose uploaders have entries at VocaDB
              </b-form-checkbox>
            </b-col>
          </b-row>
        </b-collapse>
      </b-col>
      <b-col class="m-auto text-left mt-lg-3 ml-n1">
        <b-button
          v-if="tagInfoLoaded()"
          variant="link"
          :disabled="defaultDisableCondition()"
          @click="loadPage(page)"
        >
          <font-awesome-icon icon="fa-solid fa-arrow-rotate-right" />
        </b-button>
      </b-col>
    </b-row>
    <b-row
      v-if="tagInfoLoaded() && isActiveMode()"
      class="mt-lg-3 pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-col class="my-auto"
        >Tag:<br /><strong>
          <b-link
            v-if="tagInfoLoaded()"
            target="_blank"
            :href="getVocaDBTagUrl(tag.id)"
            >{{ tag.name }}</b-link
          >
        </strong></b-col
      >
      <b-col class="my-auto"
        >Search expression:<br /><strong>
          <b-link
            v-if="tagInfoLoaded()"
            target="_blank"
            :href="
              getNicoTagUrl(tagMappings.join(' OR '), scopeTagStringFrozen)
            "
            >view at NND
            <font-awesome-icon
              class="ml-1"
              icon="fas fa-external-link"
            /> </b-link></strong
      ></b-col>
      <b-col class="my-auto"
        >Videos found:<br /><strong
          >{{ totalVideoCount
          }}<b-iconstack v-if="scopeTagStringFrozen != ''" class="ml-1">
            <b-icon stacked icon="circle" scale="1.20"></b-icon>
            <b-icon
              stacked
              icon="circle-fill"
              scale="0.8"
            ></b-icon></b-iconstack
          ><b-icon
            v-else
            class="ml-1"
            icon="circle-fill"
            scale="1.20"
          ></b-icon></strong
      ></b-col>
      <b-col class="my-auto">
        <b-dropdown
          block
          :disabled="defaultDisableCondition()"
          :text="getResultNumberStr()"
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
        <b-button
          :disabled="defaultDisableCondition()"
          variant="primary"
          block
          :pressed.sync="noEntry"
          @click="filterVideos()"
          >Videos without entries
        </b-button>
      </b-col>
      <b-col class="my-auto">
        <b-button
          :disabled="defaultDisableCondition()"
          variant="primary"
          block
          :pressed.sync="tagged"
          @click="filterVideos()"
          >Tagged songs
        </b-button>
      </b-col>
    </b-row>
    <b-row v-if="tagInfoLoaded() && isActiveMode()">
      <b-col class="col-12">
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
                filterVideos();
              "
              >{{ songType.type }} ({{ songType.count }})
            </b-button>
          </b-button-group>
        </div>
      </b-col>
    </b-row>
    <b-row v-if="tagInfoLoaded() && isActiveMode()">
      <template>
        <div class="overflow-auto m-auto mt-lg-3">
          <b-pagination
            v-model="page"
            align="center"
            :total-rows="totalVideoCount"
            :per-page="maxResults"
            use-router
            first-number
            last-number
            limit="10"
            :disabled="defaultDisableCondition()"
            @input="loadPage(page)"
          ></b-pagination>
        </div>
      </template>
    </b-row>
    <b-row v-if="tagInfoLoaded() && isActiveMode()">
      <b-table-simple hover class="mt-1 col-lg-12">
        <b-thead>
          <b-th>
            <b-form-checkbox
              v-model="allChecked"
              size="lg"
              :disabled="defaultDisableCondition() || noVideosWithEntries()"
              @change="toggleCheckAll"
            ></b-form-checkbox>
          </b-th>
          <b-th class="col-8 align-middle">Video</b-th>
          <b-th class="col-3 align-middle">Entry</b-th>
          <b-th class="col-1 align-middle">Tag</b-th>
        </b-thead>
        <b-tbody v-if="!allInvisible()">
          <b-tr
            v-for="(item, key) in videos"
            :id="item.video.id"
            :key="key"
            :style="item.visible ? '' : 'display: none'"
          >
            <b-td>
              <div v-if="item.entry != null && !item.processed">
                <b-form-checkbox
                  v-model="item.selected"
                  size="lg"
                  :disabled="defaultDisableCondition()"
                ></b-form-checkbox>
              </div>
            </b-td>
            <b-td>
              <b-button
                :disabled="defaultDisableCondition()"
                size="sm"
                variant="primary-outline"
                class="mr-2"
                @click="item.video.visible = !item.video.visible"
              >
                <font-awesome-icon icon="fas fa-play" />
              </b-button>
              <b-link target="_blank" :href="getNicoVideoUrl(item.video.id)">{{
                item.video.title
              }}</b-link>
              <b-badge
                v-clipboard:copy="item.video.length"
                variant="primary"
                class="m-sm-1"
                href="#"
                >{{ item.video.length }}</b-badge
              >
              <div>
                <b-badge
                  v-for="tag in item.video.tags"
                  :key="tag.name"
                  v-clipboard:copy="tag.name"
                  class="m-sm-1"
                  :variant="tag.type"
                  href="#"
                >
                  <font-awesome-icon icon="fas fa-tag" class="mr-1" />
                  {{ tag.name }}
                </b-badge>
              </div>
              <b-collapse
                :visible="item.video.visible && !fetching"
                class="mt-2 collapsed"
              >
                <nico-embed
                  v-if="item.video.visible && !fetching"
                  :content-id="item.video.id"
                />
              </b-collapse>
              <nico-description
                :content-id="item.video.id"
                :description="item.video.description"
                :publisher="item.publisher"
              />
            </b-td>
            <b-td>
              <div v-if="item.entry != null">
                <b-link target="_blank" :href="getVocaDBEntryUrl(item.entry.id)"
                  >{{ item.entry.name
                  }}<b-badge
                    class="badge text-center ml-2"
                    :variant="getSongTypeColorForDisplay(item.entry.type)"
                  >
                    {{ getShortenedSongType(item.entry.type) }}
                  </b-badge></b-link
                >
                <div class="text-muted">
                  {{ item.entry.artistString }}
                </div>
                <entry-error-report :error-report="item.errorReport" />
              </div>
              <div v-else>
                <add-to-the-database
                  :disabled="fetching"
                  :client-type="clientType"
                  :publisher="item.publisher"
                  :video-id="item.video.id"
                />
              </div>
            </b-td>
            <b-td>
              <div v-if="item.entry != null">
                <b-button-toolbar key-nav>
                  <b-button
                    v-if="item.processed"
                    style="pointer-events: none"
                    class="btn disabled"
                    variant="success"
                  >
                    <font-awesome-icon icon="fas fa-check" />
                  </b-button>
                  <b-button
                    v-else
                    :disabled="defaultDisableCondition()"
                    class="btn"
                    variant="outline-success"
                    @click="updateSingle(item)"
                  >
                    <font-awesome-icon icon="fas fa-plus" />
                  </b-button>
                </b-button-toolbar>
              </div>
            </b-td>
          </b-tr>
        </b-tbody>
        <b-tbody v-else>
          <b-tr>
            <b-td colspan="4" class="text-center text-muted">
              <small>No items to display</small>
            </b-td>
          </b-tr>
        </b-tbody>
        <b-tfoot>
          <b-th>
            <b-form-checkbox
              v-model="allChecked"
              size="lg"
              :disabled="defaultDisableCondition() || noVideosWithEntries()"
              @change="toggleCheckAll"
            ></b-form-checkbox>
          </b-th>
          <b-th class="col-8 align-middle">Video</b-th>
          <b-th class="col-3 align-middle">Entry</b-th>
          <b-th class="col-1 align-middle">Tag</b-th>
        </b-tfoot>
      </b-table-simple>
    </b-row>
    <b-row
      v-if="tagInfoLoaded() && isActiveMode()"
      class="mt-lg-1 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
    >
      <b-col class="col-lg-3 m-auto">
        <b-button
          block
          variant="primary"
          :disabled="countChecked() === 0 || massAssigning || fetching"
          @click="updateMultiple"
        >
          <div v-if="massAssigning">
            <b-spinner small class="mr-1"></b-spinner>
            Assigning...
          </div>
          <div v-else>Batch assign ({{ countChecked() }} selected)</div>
        </b-button>
      </b-col>
    </b-row>
    <b-row v-if="tagInfoLoaded() && isActiveMode()">
      <div class="overflow-auto m-auto mt-lg-4">
        <b-pagination
          v-model="page"
          class="mb-5"
          align="center"
          :total-rows="totalVideoCount"
          :per-page="maxResults"
          use-router
          first-number
          last-number
          limit="10"
          :disabled="defaultDisableCondition()"
          @input="loadPage(page)"
        ></b-pagination>
      </div>
    </b-row>
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import {
  allVideosInvisible,
  getErrorData,
  getClientType,
  getMaxResultsForDisplay,
  getNicoTagUrl,
  getNicoVideoUrl,
  getShortenedSongType,
  getSongTypeColorForDisplay,
  getSortingConditionForDisplayNico,
  getUniqueElementId,
  getVocaDBAddSongUrl,
  getVocaDBArtistUrl,
  getVocaDBSongUrl,
  getVocaDBTagUrl,
  infoLoaded,
  mapSongTypeStats,
  pageStateIsValid
} from "@/utils";
import { api } from "@/backend";
import ErrorMessage from "@/components/ErrorMessage.vue";
import { AxiosError, AxiosResponse } from "axios";
import NicoEmbed from "@/components/NicoEmbed.vue";
import NicoDescription from "@/components/NicoDescription.vue";
import EntryErrorReport from "@/components/EntryErrorReport.vue";
import { ClientType, NndSortOrder, SongType } from "@/backend/dto/enumeration";
import { SongTypeStatsRecord } from "@/backend/dto/songTypeStats";
import { VocaDbTag } from "@/backend/dto/lowerLevelStruct";
import { SongTagsAndPvsMassUpdateRequest } from "@/backend/dto/request/songTagsAndPvsUpdateRequest";
import { NndVideoWithAssociatedVocaDbEntryForTag } from "@/backend/dto/higherLevelStruct";
import {
  defaultScopeTagString,
  localStorageKeyDbTag,
  localStorageKeyDbTagScope,
  localStorageKeyMaxResults,
  localStorageKeyNndOrderBy,
  maxResultsOptions,
  nndOrderOptions
} from "@/constants";
import AddToTheDatabase from "@/components/AddToTheDatabase.vue";

@Component({
  components: {
    AddToTheDatabase,
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

  @Prop()
  private readonly targName: string | undefined;

  private readonly orderOptionsNico = nndOrderOptions;
  private readonly maxResultsOptions: number[] = maxResultsOptions;

  // main variables
  private tag: VocaDbTag = { id: -1, name: "" };
  private tagName: string = "";
  private scopeTagString: string = "";
  private scopeTagStringFrozen: string = "";
  private videos: NndVideoWithAssociatedVocaDbEntryForTag[] = [];

  // api variables
  private pageToJump: number = 1;
  private maxResults: number = 10;
  private sortingCondition: NndSortOrder = "startTime";
  private fetching: boolean = false;
  private massAssigning: boolean = false;
  private assigning: boolean = false;
  private clientType: ClientType = getClientType();
  private chunkSize = 10;

  // interface variables
  private allChecked: boolean = false;
  private showCollapse: boolean = false;
  private totalVideoCount: number = 0;
  private page: number = 0;
  private maxPage: number = 0;
  private noEntry: boolean = false;
  private showVideosWithUploaderEntry: boolean = true;
  private tagged: boolean = false;
  private tagMappings: string[] = [];
  private songTypeStats: SongTypeStatsRecord[] = [];

  // error handling
  private alertStatusText = "";
  private alertMessage: string = "";

  // interface methods
  private isActiveMode(): boolean {
    return this.mode == this.thisMode;
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning || this.assigning;
  }

  private setMaxResults(maxResults: number): void {
    this.maxResults = maxResults;
    localStorage.setItem(localStorageKeyMaxResults, this.maxResults.toString());
  }

  private getHiddenTypes(): number {
    return this.songTypeStats.filter(t => !t.show).length;
  }

  private setDefaultScopeTagString(): void {
    this.scopeTagString = defaultScopeTagString;
  }

  private getSortingCondition(): string {
    return getSortingConditionForDisplayNico(this.sortingCondition);
  }

  private setSortingCondition(value: NndSortOrder): void {
    this.sortingCondition = value;
    localStorage.setItem(localStorageKeyNndOrderBy, this.sortingCondition);
  }

  private noVideosWithEntries(): boolean {
    return this.videos.every(video => video.entry == null);
  }

  private toggleCheckAll(): void {
    for (const item of this.videos) {
      item.selected =
        this.allChecked &&
        item.visible &&
        item.entry != null &&
        !item.processed;
    }
  }

  private countChecked(): number {
    return this.videos.filter(video => video.selected).length;
  }

  // row filtering
  private hiddenTypeFlag(
    video: NndVideoWithAssociatedVocaDbEntryForTag
  ): boolean {
    return (
      this.getHiddenTypes() == 0 ||
      video.entry == null ||
      (video.entry != null &&
        !this.songTypeStats
          .filter(statsItem => !statsItem.show)
          .map(statsItem => statsItem.type)
          .includes(video.entry.type))
    );
  }

  private noEntryFlag(video: NndVideoWithAssociatedVocaDbEntryForTag): boolean {
    return (
      video.entry != null ||
      this.noEntry ||
      (video.publisher?.type == "DATABASE" && this.showVideosWithUploaderEntry)
    );
  }

  private taggedFlag(video: NndVideoWithAssociatedVocaDbEntryForTag): boolean {
    return video.entry == null || this.tagged || !video.processed;
  }

  private filterVideos(): void {
    for (const item of this.videos) {
      item.visible =
        this.hiddenTypeFlag(item) &&
        this.noEntryFlag(item) &&
        this.taggedFlag(item);
      item.selected = item.selected && item.visible;
    }
  }

  // proxy methods
  private tagInfoLoaded(): boolean {
    return infoLoaded(this.videos.length, this.tag.name);
  }

  private getVocaDBTagUrl(id: number): string {
    return getVocaDBTagUrl(this.clientType, id);
  }

  private getNicoTagUrl(tagName: string, scopeTag: string): string {
    return getNicoTagUrl(tagName, scopeTag);
  }

  private getResultNumberStr(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }

  private getSongTypeColorForDisplay(songType: string): string {
    return getSongTypeColorForDisplay(SongType[songType]);
  }

  private pageStateIsValid(): boolean {
    return pageStateIsValid(this.pageToJump, this.maxPage);
  }

  private allInvisible(): boolean {
    return allVideosInvisible(this.videos);
  }

  private getNicoVideoUrl(contentId: string): string {
    return getNicoVideoUrl(contentId);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBSongUrl(this.clientType, id);
  }

  private getShortenedSongType(songType: string): string {
    return getShortenedSongType(SongType[songType]);
  }

  private getVocaDBAddSongUrl(contentId: string): string {
    return getVocaDBAddSongUrl(this.clientType, contentId);
  }

  private getVocaDBArtistUrl(artistId: number): string {
    return getVocaDBArtistUrl(this.clientType, artistId);
  }

  private updateUrl(): void {
    this.$router
      .push({
        name: "tags-full",
        params: { browseMode: this.thisMode, targName: this.tagName }
      })
      .catch(err => {
        return false;
      });
  }

  // api methods
  async fetch(
    targetTag: string,
    scopeString: string,
    newStartOffset: number,
    newPage: number
  ): Promise<void> {
    this.showCollapse = false;
    this.fetching = true;
    try {
      const response = await api.getVideosByVocaDbTags({
        tag: targetTag,
        scope: scopeString,
        startOffset: newStartOffset,
        maxResults: this.maxResults,
        orderBy: this.sortingCondition,
        clientType: this.clientType
      });
      this.videos = response.items.map(item => {
        return {
          video: {
            id: item.video.id,
            title: item.video.title,
            description: item.video.description,
            tags: item.video.tags,
            length: item.video.length,
            visible: false
          },
          entry: item.entry,
          publisher: item.publisher,
          visible: true,
          selected: false,
          processed: !item.entry?.mappedTags.some(tag => !tag.selected),
          errorReport: null
        };
      });
      this.songTypeStats = mapSongTypeStats(
        response.songTypeStats,
        this.songTypeStats
      );
      this.filterVideos();
      this.tagMappings = response.tagMappings;
      this.totalVideoCount = response.totalCount;
      this.scopeTagString = response.cleanScope;
      this.scopeTagStringFrozen = response.cleanScope;
      this.tag = response.tag;
      this.tagName = response.tag.name;
      this.tagMappings = response.tagMappings;
      this.scopeTagString = scopeString;
      this.scopeTagStringFrozen = scopeString;
      this.allChecked = false;
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.pageToJump = newPage;
      this.page = newPage;
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      localStorage.setItem(localStorageKeyDbTag, targetTag);
      localStorage.setItem(localStorageKeyDbTagScope, scopeString);
      localStorage.setItem(
        localStorageKeyMaxResults,
        this.maxResults.toString()
      );
      localStorage.setItem(localStorageKeyNndOrderBy, this.sortingCondition);
      this.fetching = false;
    }
  }

  private async updateSingle(
    video: NndVideoWithAssociatedVocaDbEntryForTag
  ): Promise<void> {
    this.assigning = true;
    try {
      await this.update([video]);
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      this.assigning = false;
    }
  }

  private async updateMultiple(): Promise<void> {
    this.massAssigning = true;
    try {
      let videosToProcess = this.videos.filter(video => video.selected);
      let startPosition = 0;
      let currentChunk = videosToProcess.slice(
        startPosition,
        startPosition + this.chunkSize
      );
      while (currentChunk.length > 0) {
        await this.update(currentChunk);
        startPosition += this.chunkSize;
        currentChunk = videosToProcess.slice(startPosition, startPosition + 10);
      }
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      this.massAssigning = false;
      this.allChecked = false;
    }
  }

  private async update(videos: NndVideoWithAssociatedVocaDbEntryForTag[]) {
    const errors = await api.updateSongTagsAndPvs(this.createRequest(videos));
    const entriesWithErrors = errors.map(error => error.entryId);
    for (const video of videos) {
      if (!entriesWithErrors.includes(video.entry?.id as number)) {
        video.selected = false;
        video.processed = true;
        video.errorReport = null;
      } else {
        video.errorReport = errors.filter(
          error => error.entryId == video.entry?.id
        )[0];
      }
    }
  }

  private createRequest(
    videos: NndVideoWithAssociatedVocaDbEntryForTag[]
  ): SongTagsAndPvsMassUpdateRequest {
    return {
      subRequests: videos.map(video => {
        return {
          songId: video.entry?.id as number,
          pvId: video.video.id,
          tags: video.entry?.mappedTags
            .filter(tagSelection => !tagSelection.selected)
            .map(tagSelection => tagSelection.tag.id) as number[],
          nndPvsToDisable: []
        };
      }),
      clientType: this.clientType
    };
  }

  private loadInitialPage(): void {
    this.updateUrl();
    this.fetch(this.tagName, this.scopeTagString, 0, 1);
  }

  private loadPage(page: number): void {
    this.updateUrl();
    this.fetch(
      this.tagName,
      this.scopeTagStringFrozen,
      (page - 1) * this.maxResults,
      page
    );
  }

  // error handling
  private processError(response: AxiosResponse | undefined): void {
    const errorData = getErrorData(response);
    this.alertMessage = errorData.message;
    this.alertStatusText = errorData.statusText;
    this.$bvToast.show(getUniqueElementId("error_", this.mode.toString()));
  }

  // session
  created(): void {
    let max_results = localStorage.getItem(localStorageKeyMaxResults);
    if (max_results != null) {
      this.maxResults = parseInt(max_results);
    }
    let sort_by = localStorage.getItem(localStorageKeyNndOrderBy);
    if (sort_by != null) {
      this.sortingCondition = sort_by as NndSortOrder;
    }
    let vocadb_mapped_tag = localStorage.getItem(localStorageKeyDbTag);
    if (vocadb_mapped_tag != null) {
      this.tagName = vocadb_mapped_tag;
    }
    let vocadb_mapped_tag_scope = localStorage.getItem(
      localStorageKeyDbTagScope
    );
    if (vocadb_mapped_tag_scope != null) {
      this.scopeTagString = vocadb_mapped_tag_scope;
    }
  }

  // fill tag name from address params (override local storage)
  mounted(): void {
    let targName = this.$route.params["targName"];
    if (targName != undefined) {
      this.tagName = targName;
    }
  }
}
</script>
