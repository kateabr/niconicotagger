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
            placeholder="NicoNicoDouga tag"
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
          <b-row v-if="tagNameFrozen !== '' && isActiveMode()" class="mt-2">
            <b-col>
              <b-dropdown
                block
                :disabled="defaultDisableCondition()"
                :text="getsortingCondition()"
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
                  :state="pageStateIsValid"
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
          <b-row v-if="tagNameFrozen !== '' && isActiveMode()" class="mt-2">
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
            :href="getNicoTagUrl(tagNameFrozen, scopeTagStringFrozen)"
            >{{ tagNameFrozen }}
            <font-awesome-icon class="ml-1" icon="fas fa-external-link" />
          </b-link> </strong
      ></b-col>
      <b-col class="my-auto"
        >Mapped to:<br />
        <strong v-for="(tag, key) in tagMappings" :key="key">
          <b-link target="_blank" :href="getVocaDBTagUrl(tag.id)">{{
            tag.name
          }}</b-link
          ><span v-if="tagMappings.length - key > 1">, </span>
        </strong>
      </b-col>
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
            @change="loadPage"
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
            v-for="(video, key) in videos"
            :id="video.video.id"
            :key="key"
            :style="video.visible ? '' : 'display: none'"
          >
            <b-td>
              <div v-if="video.entry != null && !video.processed">
                <b-form-checkbox
                  v-model="video.selected"
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
                @click="video.video.visible = !video.video.visible"
              >
                <font-awesome-icon icon="fas fa-play" />
              </b-button>
              <b-link target="_blank" :href="getNicoVideoUrl(video.video.id)">{{
                video.video.title
              }}</b-link>
              <b-badge
                v-clipboard:copy="video.video.length"
                variant="primary"
                class="m-sm-1"
                href="#"
                >{{ video.video.length }}</b-badge
              >
              <div>
                <b-badge
                  v-for="(tag, tagKey) in video.video.tags"
                  :key="tagKey"
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
                :visible="video.video.visible && !fetching"
                class="mt-2 collapsed"
              >
                <nico-embed
                  v-if="video.video.visible && !fetching"
                  :content-id="video.video.id"
                />
              </b-collapse>
              <nico-description
                :content-id="video.video.id"
                :description="video.video.description"
                :publisher="video.publisher"
              />
            </b-td>
            <b-td>
              <div v-if="video.entry != null">
                <b-link
                  target="_blank"
                  :href="getVocaDBEntryUrl(video.entry.id)"
                  >{{ video.entry.name
                  }}<b-badge
                    class="badge text-center ml-2"
                    :variant="getSongTypeColorForDisplay(video.entry.type)"
                  >
                    {{ getShortenedSongType(video.entry.type) }}
                  </b-badge></b-link
                >
                <div class="text-muted">
                  {{ video.entry.artistString }}
                </div>
              </div>
              <div v-else>
                <b-button
                  size="sm"
                  :disabled="fetching"
                  :href="getVocaDBAddSongUrl(video.video.id)"
                  target="_blank"
                  >Add to the database
                </b-button>
                <div
                  v-if="
                    video.publisher !== null &&
                    video.publisher.type == 'DATABASE'
                  "
                  class="small text-secondary"
                >
                  Published by
                  <b-link
                    target="_blank"
                    :href="getVocaDBArtistUrl(video.publisher.id)"
                    >{{ video.publisher.name }}</b-link
                  >
                </div>
              </div>
              <entry-error-report :error-report="video.errorReport" />
            </b-td>
            <b-td>
              <div v-if="video.entry != null">
                <b-button-toolbar key-nav>
                  <b-button
                    v-if="video.processed"
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
                    @click="updateSingle(video)"
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
          @change="loadPage"
        ></b-pagination>
      </div>
    </b-row>
  </span>
</template>

<script lang="ts">
import { Component, Prop } from "vue-property-decorator";
import Vue from "vue";
import { api } from "@/backend";
import {
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
import NicoEmbed from "@/components/NicoEmbed.vue";
import ErrorMessage from "@/components/ErrorMessage.vue";
import { AxiosError, AxiosResponse } from "axios";
import NicoDescription from "@/components/NicoDescription.vue";
import EntryErrorReport from "@/components/EntryErrorReport.vue";
import { ClientType, NndSortOrder, SongType } from "@/backend/dto/enumeration";
import { SongTypeStatsRecord } from "@/backend/dto/songTypeStats";
import { VocaDbTag } from "@/backend/dto/lowerLevelStruct";
import { SongTagsAndPvsMassUpdateRequest } from "@/backend/dto/request/songTagsAndPvsUpdateRequest";
import { NndVideoWithAssociatedVocaDbEntryForTag } from "@/backend/dto/higherLevelStruct";
import {
  defaultScopeTagString,
  localStorageKeyMaxResults,
  localStorageKeyNndOrderBy,
  localStorageKeyNndTag,
  localStorageKeyNndTagScope,
  maxResultsOptions,
  nndOrderOptions
} from "@/constants";

@Component({
  components: { EntryErrorReport, NicoDescription, NicoEmbed, ErrorMessage }
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
  private tagName: string = "";
  private tagNameFrozen: string = "";
  private scopeTagString: string = "";
  private scopeTagStringFrozen: string = "";
  private videos: NndVideoWithAssociatedVocaDbEntryForTag[] = [];
  private tagMappings: VocaDbTag[] = [];
  private songTypeStats: SongTypeStatsRecord[] = [];

  // api variables
  private pageToJump: number = 1;
  private startOffset: number = 0;
  private maxResults: number = 10;
  private sortingCondition: NndSortOrder = "startTime";
  private fetching: boolean = false;
  private massAssigning: boolean = false;
  private assigning: boolean = false;
  private clientType: ClientType = getClientType();

  // interface variables
  private allChecked: boolean = false;
  private showCollapse: boolean = false;
  private totalVideoCount: number = 0;
  private page: number = 0;
  private maxPage: number = 0;
  private noEntry: boolean = false;
  private showVideosWithUploaderEntry: boolean = true;
  private tagged: boolean = false;

  // error handling
  private alertStatusText = "";
  private alertMessage: string = "";

  // proxy methods
  private pageStateIsValid(): boolean {
    return pageStateIsValid(this.pageToJump, this.maxPage);
  }

  private tagInfoLoaded(): boolean {
    return infoLoaded(this.videos.length, this.tagNameFrozen);
  }

  private getsortingCondition(): string {
    return getSortingConditionForDisplayNico(this.sortingCondition);
  }

  private setDefaultScopeTagString(): void {
    this.scopeTagString = defaultScopeTagString;
  }

  private getNicoTagUrl(tagName: string, scopeTag: string): string {
    return getNicoTagUrl(tagName, scopeTag);
  }

  private getVocaDBTagUrl(id: number): string {
    return getVocaDBTagUrl(this.clientType, id);
  }

  private getResultNumberStr(): string {
    return getMaxResultsForDisplay(this.maxResults);
  }

  private getSongTypeColorForDisplay(songType: string): string {
    return getSongTypeColorForDisplay(SongType[songType]);
  }

  private allInvisible(): boolean {
    return !this.videos.some(video => video.visible);
  }

  private getNicoVideoUrl(contentId: string): string {
    return getNicoVideoUrl(contentId);
  }

  private getVocaDBEntryUrl(id: number): string {
    return getVocaDBSongUrl(this.clientType, id);
  }

  private getShortenedSongType(songType: SongType): string {
    return getShortenedSongType(songType);
  }

  private getVocaDBAddSongUrl(contentId: string): string {
    return getVocaDBAddSongUrl(this.clientType, contentId);
  }

  private getVocaDBArtistUrl(artistId: number): string {
    return getVocaDBArtistUrl(this.clientType, artistId);
  }

  // interface methods
  private isActiveMode(): boolean {
    return this.mode == this.thisMode;
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning || this.assigning;
  }

  private getHiddenTypes(): number {
    return this.songTypeStats.filter(statsItem => !statsItem.show).length;
  }

  private setSortingCondition(value: NndSortOrder): void {
    this.sortingCondition = value;
    localStorage.setItem(localStorageKeyNndOrderBy, this.sortingCondition);
  }

  private setMaxResults(maxResults: number): void {
    this.maxResults = maxResults;
    localStorage.setItem(localStorageKeyMaxResults, this.maxResults.toString());
  }

  private noVideosWithEntries(): boolean {
    return this.videos.every(video => video.entry == null);
  }

  private toggleCheckAll(): void {
    for (const item of this.videos.filter(
      video => video.visible && video.entry != null && !video.processed
    )) {
      item.selected = this.allChecked;
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
      !this.songTypeStats
        .filter(statsItem => !statsItem.show)
        .map(statsItem => statsItem.type)
        .includes(video.entry.type)
    );
  }

  private noEntryFlag(video: NndVideoWithAssociatedVocaDbEntryForTag): boolean {
    return (
      this.noEntry ||
      video.entry != null ||
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
    if (targetTag == "") {
      return;
    }
    this.showCollapse = false;
    this.fetching = true;
    try {
      let response = await api.getVideosByNndTagsForTagging({
        tags: [targetTag],
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
            length: item.video.length,
            tags: item.video.tags,
            visible: false
          },
          entry: item.entry,
          publisher: item.publisher,
          selected: false,
          visible: true,
          processed:
            item.entry != null &&
            !item.entry.mappedTags.some(tag => !tag.selected),
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
      this.tagMappings = response.tagMappings;
      this.tagName = targetTag;
      this.tagNameFrozen = targetTag;
      this.startOffset = newStartOffset;
      this.allChecked = false;
    } catch (err) {
      this.processError((err as AxiosError).response);
    } finally {
      localStorage.setItem(localStorageKeyNndTag, targetTag);
      localStorage.setItem(localStorageKeyNndTagScope, scopeString);
      localStorage.setItem(
        localStorageKeyMaxResults,
        this.maxResults.toString()
      );
      localStorage.setItem(localStorageKeyNndOrderBy, this.sortingCondition);
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.fetching = false;
      this.pageToJump = newPage;
      this.page = newPage;
    }
  }

  private async updateSingle(
    video: NndVideoWithAssociatedVocaDbEntryForTag
  ): Promise<void> {
    if (video.entry == null) {
      return;
    }
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
      await this.update(this.videos.filter(video => video.selected));
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
      this.tagNameFrozen,
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
    let nicovideo_mapped_tag = localStorage.getItem(localStorageKeyNndTag);
    if (nicovideo_mapped_tag != null) {
      this.tagName = nicovideo_mapped_tag;
    }
    let nicovideo_mapped_tag_scope = localStorage.getItem(
      localStorageKeyNndTagScope
    );
    if (nicovideo_mapped_tag_scope != null) {
      this.scopeTagString = nicovideo_mapped_tag_scope;
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
