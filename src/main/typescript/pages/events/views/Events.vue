<template>
  <div style="display: flex; align-items: center">
    <b-container class="col-lg-11">
      <b-toaster class="b-toaster-top-center" name="toaster-2"></b-toaster>
      <b-toast
        id="error"
        title="Error"
        no-auto-hide
        variant="danger"
        class="m-0 rounded-0"
        toaster="toaster-2"
      >
        <span v-if="alertCode !== 401">
          {{ alertMessage }}
        </span>
        <span v-else>
          Access token has expired.
          <b-link to="login" target="_blank">
            Relogin
            <font-awesome-icon class="ml-0" icon="fas fa-external-link"/>
          </b-link>
          and try again
        </span>
      </b-toast>
      <b-tabs v-model="browseMode" class="mt-3" content-class="mt-3">
        <b-tab title="Browse by event tag (VocaDB)">
          <b-row>
            <b-row
              class="pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
            >
              <b-col
              >
                <b-row>
                  <b-col class="my-auto">
                    <b-dropdown
                      :disabled="
                              defaultDisableCondition() ||
                              !activeMode(0)
                            "
                      block
                      :text="getResultNumberStr()"
                      class="my-auto"
                      variant="primary"
                    >
                      <b-dropdown-item
                        :disabled="maxResults === 10"
                        @click="setMaxResults(10)"
                      >10
                      </b-dropdown-item>
                      <b-dropdown-item
                        :disabled="maxResults === 25"
                        @click="setMaxResults(25)"
                      >25
                      </b-dropdown-item>
                      <b-dropdown-item
                        :disabled="maxResults === 50"
                        @click="setMaxResults(50)"
                      >50
                      </b-dropdown-item>
                      <b-dropdown-item
                        :disabled="maxResults === 100"
                        @click="setMaxResults(100)"
                      >100
                      </b-dropdown-item>
                    </b-dropdown>
                  </b-col>
                  <b-col class="my-auto">
                    <b-dropdown
                      block
                      :disabled="
                              defaultDisableCondition() ||
                              !activeMode(0)
                            "
                      :text="getOrderingCondition()"
                      variant="primary"
                    >
                      <b-dropdown-item
                        v-for="(key, value) in orderOptions"
                        :key="key"
                        :disabled="orderBy === value"
                        @click="setOrderBy(value)"
                      >
                        {{ orderOptions[value] }}
                      </b-dropdown-item>
                    </b-dropdown>
                  </b-col>
                  <b-col v-if="showTable0" class="my-auto">
                    <template>
                      <b-input-group
                        inline
                        :state="pageState"
                        invalid-feedback="Wrong page number"
                      >
                        <template #prepend>
                          <b-input-group-text
                            class="justify-content-center"
                            style="width: 80px"
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
                            :state="pageState()"
                            @keydown.enter.native="
                                    pageState()
                                      ? fetch1(event.name,
                                          (pageToJump - 1) * maxResults,
                                          pageToJump
                                        )
                                      : null
                                  "
                          >
                          </b-form-input>
                        </template>
                        <template #append>
                          <b-button
                            style="width: 80px"
                            :variant="pageState() ? 'success' : 'danger'"
                            :disabled="
                                    defaultDisableCondition() || !pageState()
                                  "
                            @click="refreshPage1"
                          >
                                  <span v-if="fetching"
                                  ><b-spinner small
                                  /></span>
                            <span v-else-if="pageToJump === page"
                            >Refresh</span
                            >
                            <span v-else>Jump</span>
                          </b-button>
                        </template>
                      </b-input-group>
                    </template>
                  </b-col>
                  <b-col v-else class="m-auto">
                    <b-button
                      v-if="activeMode(1) || videos.length > 0"
                      variant="primary"
                      block
                      :disabled="defaultDisableCondition()"
                      @click="fetch1(eventTagName, 0, 1)"
                    ><span v-if="fetching"><b-spinner small/></span>
                      <span v-else>Load</span>
                    </b-button>
                  </b-col>
                  <b-col class="my-auto">
                    <b-button
                      variant="primary"
                      block
                      :pressed="showEntriesMatchingEvents"
                      :disabled="defaultDisableCondition()"
                      @click="filter(false)"
                    >Entries with matching events
                    </b-button>
                  </b-col>
                  <b-col class="my-auto">
                    <b-button
                      variant="primary"
                      block
                      :pressed="showEntriesWithNoTags"
                      :disabled="defaultDisableCondition()"
                      @click="filterVideos()"
                    >Entries with no tags to add
                    </b-button>
                  </b-col>
                </b-row>
                <b-row class="mt-3">
                  <b-col class="my-auto">
                    <template>
                      <b-input-group inline>
                        <b-form-input
                          v-model="eventTagName"
                          :readonly="fetching || !activeMode(0)"
                          type="text"
                        />
                        <template #prepend>
                          <b-input-group-text
                            class="justify-content-center"
                          >Event tag
                          </b-input-group-text>
                        </template>
                        <template v-if="event.id > 0" #append class="col-5">
                          <b-input-group-text
                            class="justify-content-center"
                          ><b-link :to="getVocaDBEvent(event)" target="_blank">{{event.name}}</b-link>
                          </b-input-group-text>
                        </template>
                      </b-input-group>
                    </template>
                  </b-col>
                  <b-col class="my-auto text-left align-middle">
                    <template>
                      <b-input-group inline>
                        <b-form-input
                          v-model="timeDelta"
                          :disabled="!timeDeltaEnabled || defaultDisableCondition() || !activeMode(0)"
                          number
                          type="range"
                          min="0"
                          max="7"
                        />
                        <template #prepend>
                          <b-input-group-text
                            class="justify-content-center"
                          >
                            <b-form-checkbox
                              :disabled="fetching || !activeMode(0)"
                              v-model="timeDeltaEnabled"
                            />
                            Time delta
                          </b-input-group-text>
                        </template>
                        <template #append>
                          <b-input-group-text
                            class="justify-content-center"
                          >{{ timeDeltaEnabled ? timeDelta : "-" }}
                            day(s)
                          </b-input-group-text>
                        </template>
                      </b-input-group>
                    </template>
                  </b-col>
                </b-row>
              </b-col>
            </b-row>
            <b-row v-if="showTable0" class="col-12">
              <b-col class="my-auto">
                <div class="text-center pt-sm-3">
                  <b-button-group>
                    <b-button
                      v-for="(type, key) in songTypes"
                      :key="key"
                      class="pl-4 pr-4"
                      :disabled="defaultDisableCondition()"
                      :variant="
                              (type.show ? '' : 'outline-') +
                              getSongTypeVariant(type.name)
                            "
                      @click="
                              type.show = !type.show;
                              filterVideos();
                            "
                    >{{ getTypeInfo(type.name) }}
                    </b-button>
                  </b-button-group>
                </div>
              </b-col>
            </b-row>
            <b-row v-if="showTable0" class="col-12">
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
                    @change="pageClicked"
                  />
                </div>
              </template>
            </b-row>
            <b-table-simple
              v-if="showTable0 && activeMode(0)"
              hover
              class="mt-1 col-lg-12"
            >
              <b-thead>
                <b-th>
                  <b-form-checkbox
                    class="invisible"
                    size="lg"
                  ></b-form-checkbox>
                </b-th>
                <b-th class="col-3 align-middle">Entry</b-th>
                <b-th class="col-5 align-middle">Release event</b-th>

              </b-thead>
              <b-tbody
                v-if="videos.filter(video => video.rowVisible).length > 0"
              >
                <tr
                  v-for="video in videos.filter(vid => vid.rowVisible)"
                  :key="video.songEntry.id"
                >
                  <td>

                  </td>
                  <td>
                    <b-link
                      target="_blank"
                      :to="getEntryUrl(video.songEntry)"
                      v-html="video.songEntry.name"
                    />
                    <b-link target="_blank" :to="getEntryUrl(video.songEntry)">
                      <b-badge
                        class="badge text-center ml-2"
                        :variant="getSongTypeVariant(video.songEntry.songType)"
                      >
                        {{ getSongType(video.songEntry.songType) }}
                      </b-badge>
                    </b-link>
                    <div class="text-muted">
                      {{ video.songEntry.artistString }}
                    </div>
                  </td>
                  <td>
                    <span v-if="video.songEntry.releaseEvent !== null" class="font-weight-bolder">
                      <b-badge v-if="video.songEntry.releaseEvent.id === event.id"
                        class="badge text-center"
                          variant="success"
                               :to="getVocaDBEvent(video.songEntry.releaseEvent)"
                               target="_blank"
                      >
                        {{ video.songEntry.releaseEvent.name }}
                        </b-badge>
                    </span>
                    <span v-else class="text-muted">Unspecified</span>
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
              v-if="showTable0"
              class="mt-lg-1 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
            >
              <b-col class="col-lg-3 m-auto">
                <b-button
                  block
                  variant="primary"
                  :disabled="
                          countChecked() === 0 || massAssigning || fetching
                        "
                  @click="assignMultiple"
                >
                  <div v-if="massAssigning">
                    <b-spinner small class="mr-1"></b-spinner>
                    Assigning...
                  </div>
                  <div v-else>
                    Batch assign ({{ countChecked() }} selected)
                  </div>
                </b-button>
              </b-col>
            </b-row>

            <b-row v-if="showTable0" class="col-12">
              <template>
                <div class="overflow-auto m-auto my-lg-3">
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
                    @change="pageClicked"
                  />
                </div>
              </template>
            </b-row>
          </b-row>
        </b-tab>
      </b-tabs>
    </b-container>
    <b-row class="fixed-top m-1" style="z-index: 1; max-width: min-content">
      <b-col class="p-0">
        <b-link to="vocadb" target="_blank">
          <b-button size="sm" style="width: 60px" variant="dark" squared
          >Toggle<br/>mode
          </b-button>
        </b-link>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import {Component} from "vue-property-decorator";
import {
  AssignableTag, EntriesWithReleaseEventTag,
  NicoVideoWithTidyTags,
  Publisher, ReleaseEventForApiContractSimplified,
  SongForApiContractSimplified, SongForApiContractSimplifiedWithReleaseEvent
} from "@/backend/dto";
import {api} from "@/backend";

import VueClipboard from "vue-clipboard2";

Vue.use(VueClipboard);

@Component({components: {}})
export default class extends Vue {
  private tag: string = "";
  private orderBy = "PublishDate";
  private orderOptions = {
    PublishDate: "upload time",
    AdditionDate: "addition time",
    RatingScore: "user rating"
  };
  private tagFrozen: string = "";
  private tagInfo: AssignableTag[] = [];
  private startOffset: number = 0;
  private maxResults: number = 10;
  private tagMappings: string[] = [];
  private songTypes: SongType[] = [
    {name: "Unspecified", show: true},
    {name: "Original", show: true},
    {name: "Remaster", show: true},
    {name: "Remix", show: true},
    {name: "Cover", show: true},
    {name: "Instrumental", show: true},
    {name: "Mashup", show: true},
    {name: "MusicPV", show: true},
    {name: "DramaPV", show: true},
    {name: "Other", show: true}
  ];
  private videos: EntryWithReleaseEventAndVisibility[] = [];
  private videosToDisplay0: EntryWithReleaseEventAndVisibility[] = [];
  private videosToDisplay1: EntryWithReleaseEventAndVisibility[] = [];
  private showTable0: boolean = false;
  private showTable1: boolean = false;
  private totalVideoCount: number = 0;
  private fetching: boolean = false;
  private noEntry: boolean = true;
  private showVideosWithUploaderEntry: boolean = false;
  private tagged: boolean = true;
  private page: number = 1;
  private numOfPages: number = 1;
  private allChecked: boolean = false;
  private massAssigning: boolean = false;
  private assigning: boolean = false;
  private alertMessage: string = "";
  private alertCode: number = 0;
  private scopeTagFrozen: string = "";
  private showCollapse: boolean = false;
  private pageToJump: number = this.page;
  private maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
  private browseMode = 0;
  private eventTagName: string = "";
  private eventTagNameFrozen: string = "";
  private timeDelta: number = 1;
  private timeDeltaEnabled: boolean = false;
  private event: ReleaseEventForApiContractSimplified = {
    name: "",
    id: -1,
    date: "",
    end_date: "",
    urlSlug: "",
  };
  private distinctSongCount: number = 0;
  private showEntriesMatchingEvents: boolean = true;
  private showEntriesWithNoTags: boolean = true;

  async fetch1(
    targetTag: string,
    newStartOffset: number,
    newPage: number
  ): Promise<void> {
    if (targetTag == "") {
      return;
    }
    this.showCollapse = false;
    this.fetching = true;
    try {
      let videos: EntryWithReleaseEventAndVisibility[] = [];
      this.distinctSongCount = 0;
      let end = false;
      let response: EntriesWithReleaseEventTag;
      this.pageToJump = newPage;
      while (this.distinctSongCount < this.maxResults && !end) {
        response = await api.fetchVideosFromDbByEventTag({
          tag: targetTag,
          startOffset: newStartOffset,
          maxResults: 10,
          orderBy: this.orderBy
        });
        console.log(response.items);
        end = response.items.length < 10;
        let videos_temp = response.items.map(vid => {
          return {
            songEntry: vid,
            rowVisible: true,
            toAssign: false
          };
        });
        console.log(videos_temp);

        if (this.distinctSongCount > 0) {
          const overlap = videos_temp.find(
            video =>
              video.songEntry.id == videos[videos.length - 1].songEntry.id
          );
          if (overlap != undefined) {
            videos_temp.splice(0, videos_temp.indexOf(overlap) + 1);
          }
        }
        this.distinctSongCount += videos_temp.length;
        videos = videos.concat(videos_temp);

        this.totalVideoCount = response.totalCount;
      }
      videos.splice(this.maxResults);
      this.videos = videos;
      this.tag = response.eventTag.name;
      this.event = response.releaseEvent;
      this.filterVideos();
      this.eventTagNameFrozen = this.event.name;
      this.page = newStartOffset / this.maxResults + 1;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.startOffset = newStartOffset;
      this.allChecked = false;
    } catch (err) {
      this.processError(err);
    } finally {
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.fetching = false;
      this.pageToJump = newPage;
      this.page = newPage;
      this.showTable1 = false;
      this.showTable0 = true;
    }
  }

  getEntryUrl(songEntry: SongForApiContractSimplifiedWithReleaseEvent): string {
    return "https://vocadb.net/S/" + songEntry.id;
  }

  getArtistUrl(artist: Publisher): string {
    return "https://vocadb.net/Ar/" + artist.id;
  }

  getVideoUrl(video: NicoVideoWithTidyTags): string {
    return "https://nicovideo.jp/watch/" + video.contentId;
  }

  getResultNumberStr(): string {
    return "Videos per page: " + this.maxResults;
  }

  getNicoTag(tag: string, scoped: boolean): string {
    if (scoped) {
      return "https://nicovideo.jp/tag/" + this.scopeTagFrozen + " " + tag;
    }
    return "https://nicovideo.jp/tag/" + tag;
  }

  getVocaDBTag(tags: AssignableTag[], key: number): string {
    return "https://vocadb.net/T/" + tags[key].id + "/" + tags[key].urlSlug;
  }

  getVocaDBEvent(event: ReleaseEventForApiContractSimplified): string {
    return "https://vocadb.net/E/" + event.id + "/" + event.urlSlug;
  }

  initPageClicked(): void {
    if (this.browseMode == 0) {
      this.fetch1(this.tag.trim(), 0, 1);
    } else if (this.browseMode == 1) {
      this.fetch1(this.tag.trim(), 0, 1);
    }
  }

  pageClicked(pgnum: number): void {
    if (this.browseMode == 0) {
      this.fetch1(this.event.name, (pgnum - 1) * this.maxResults, pgnum);
    } else if (this.browseMode == 1) {
      this.fetch1(this.event.name, (pgnum - 1) * this.maxResults, pgnum);
    }
  }

  setMaxResults(mxres: number): void {
    this.maxResults = mxres;
  }

  private async assign(id: number): Promise<void> {
    // this.assigning = true;
    // try {
    //   await api.assignTag({tags: this.tagInfo, songId: id});
    //   let songEntry = null;
    //   if (this.browseMode == 0) {
    //     songEntry = this.videosToDisplay0.filter(video => {
    //       if (video.songEntry == null) return false;
    //       return video.songEntry.id == id;
    //     })[0].songEntry as SongForApiContractSimplified;
    //   } else if (this.browseMode == 1) {
    //     songEntry = this.videosToDisplay1.filter(video => {
    //       if (video.songEntry == null) return false;
    //       return video.songEntry.id == id;
    //     })[0].songEntry as SongForApiContractSimplified;
    //   }
    //   if (songEntry != null) {
    //     songEntry.tagInTags = true;
    //   }
    // } catch (err) {
    //   this.processError(err);
    // } finally {
    //   this.assigning = false;
    // }
  }

  private async assignMultiple(): Promise<void> {
    // this.massAssigning = true;
    // try {
    //   if (this.browseMode == 0) {
    //     for (const vid1 of this.videosToDisplay0.filter(vid => vid.toAssign)) {
    //       let songEntry = vid1.songEntry as SongForApiContractSimplified;
    //       await this.assign(songEntry.id);
    //       vid1.toAssign = false;
    //     }
    //   } else if (this.browseMode == 1) {
    //     for (const vid1 of this.videosToDisplay1.filter(vid => vid.toAssign)) {
    //       let songEntry = vid1.songEntry as SongForApiContractSimplified;
    //       await this.assign(songEntry.id);
    //       vid1.toAssign = false;
    //     }
    //   }
    // } finally {
    //   this.massAssigning = false;
    //   this.allChecked = false;
    // }
  }

  getButtonId(song: SongForApiContractSimplified): string {
    return "assign_" + song.id;
  }

  getCollapseId(videoId: string): string {
    return "collapse_" + videoId;
  }

  getEmbedAddr(videoId: string): string {
    return (
      "https://embed.nicovideo.jp/watch/" +
      videoId +
      "?noRelatedVideo=1&enablejsapi=0"
    );
  }

  private checkAll(): void {
    // if (this.browseMode == 0) {
    //   this.videosToDisplay0
    //     .filter(video => video.songEntry != null && !video.songEntry.tagInTags)
    //     .forEach(video => (video.toAssign = this.allChecked));
    // } else if (this.browseMode == 1) {
    //   this.videosToDisplay1
    //     .filter(video => video.songEntry != null && !video.songEntry.tagInTags)
    //     .forEach(video => (video.toAssign = this.allChecked));
    // }
  }

  private countChecked(): number {
    if (this.browseMode == 0) {
      return this.videosToDisplay0.filter(video => video.toAssign).length;
    } else if (this.browseMode == 1) {
      return this.videosToDisplay1.filter(video => video.toAssign).length;
    }
    return -1;
  }

  getSongType(typeString: string): string {
    if (typeString == "Unspecified") {
      return "?";
    } else if (typeString == "MusicPV") {
      return "PV";
    } else {
      return typeString[0];
    }
  }

  getSongTypeVariant(typeString: string): string {
    if (typeString == "Original" || typeString == "Remaster") {
      return "primary";
    } else if (
      typeString == "Remix" ||
      typeString == "Cover" ||
      typeString == "Mashup" ||
      typeString == "Other"
    ) {
      return "secondary";
    } else if (typeString == "Instrumental") {
      return "dark";
    } else if (typeString == "MusicPV" || typeString == "DramaPV") {
      return "success";
    } else {
      return "warning";
    }
  }

  getAddSongUrl(pvLink: string): string {
    return (
      "https://vocadb.net/Song/Create?PVUrl=https://www.nicovideo.jp/watch/" +
      pvLink
    );
  }

  private setOrderBy(value: string): void {
    this.orderBy = value;
  }

  private filterVideos(): void {
    for (const video of this.videos) {
      video.rowVisible =
        ((this.hiddenTypes() == 0 ||
            !this.songTypes
              .filter(t => !t.show)
              .map(t => t.name)
              .includes(video.songEntry.songType)) &&
          (this.showEntriesWithNoTags));
    }
  }

  private allInvisible(list: EntryWithReleaseEventAndVisibility[]): boolean {
    return list.every(item => !item.rowVisible);
  }

  pageState(): boolean {
    return this.pageToJump > 0 && this.pageToJump <= this.maxPage;
  }

  private hiddenTypes(): number {
    return this.songTypes.filter(t => !t.show).length;
  }

  private getOrderingCondition(): string {
    return "Arrange videos by: " + this.orderOptions[this.orderBy];
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning || this.assigning;
  }

  private getTypeInfo(type: string): string {
    if (this.browseMode == 0) {
      return (
        type +
        " (" +
        this.videosToDisplay0.filter(
          vid => vid.songEntry != null && vid.songEntry.songType == type
        ).length +
        ")"
      );
    } else if (this.browseMode == 1) {
      return (
        type +
        " (" +
        this.videosToDisplay1.filter(
          vid => vid.songEntry != null && vid.songEntry.songType == type
        ).length +
        ")"
      );
    }
    return " (-1)";
  }

  private activeMode(mode: number): boolean {
    if (mode == 0) {
      return this.videosToDisplay1.length == 0;
    } else if (mode == 1) {
      return this.videosToDisplay0.length == 0;
    } else {
      return false;
    }
  }

  private notEmptyTagInfo(): boolean {
    return this.tagInfo.length > 0;
  }

  private noVideosWithEntries(): boolean {
    if (this.browseMode == 0) {
      return this.videosToDisplay0.every(video => video.songEntry == null);
    } else if (this.browseMode == 1) {
      return this.videosToDisplay1.every(video => video.songEntry == null);
    } else {
      return false;
    }
  }

  private refreshPage1() {
    this.fetch1(this.eventTagNameFrozen, (this.pageToJump - 1) * this.maxResults, this.pageToJump);
  }

  private processError(err: any): void {
    this.$bvToast.show("error");
    if (err.response == undefined) {
      this.alertCode = 0;
      this.alertMessage = err.message;
    } else {
      this.alertCode = err.response.data.code;
      this.alertMessage = err.response.data.message;
    }
  }
}

export interface EntryWithReleaseEventAndVisibility {
  songEntry: SongForApiContractSimplifiedWithReleaseEvent;
  rowVisible: boolean;
  toAssign: boolean;
}

export interface SongType {
  name: string;
  show: boolean;
}
</script>
