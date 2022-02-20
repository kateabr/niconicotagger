<template>
  <div>
    <div class="flex-fill fixed-top mb-3">
      <b-progress
        height="5px"
        :value="distinct_song_count"
        :max="maxResults"
        :precision="0"
        :animated="fetching"
        striped
      />
    </div>
    <b-row class="col-12 m-0">
      <b-col>
        <div style="display: flex; align-items: center">
          <b-container class="col-lg-11">
            <b-toaster
              class="b-toaster-top-center"
              name="toaster-2"
            ></b-toaster>
            <b-tabs v-model="browseMode" class="mt-3" content-class="mt-3">
              <b-tab title="Browse consecutively" active>
                <b-row>
                  <b-row
                    class="pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
                  >
                    <b-col class="my-auto">
                      <b-dropdown
                        :disabled="defaultDisableCondition()"
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
                        :disabled="defaultDisableCondition()"
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
                                  ? fetch0(
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
                              @click="refreshPage0"
                            >
                              <span v-if="fetching"><b-spinner small /></span>
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
                        variant="primary"
                        block
                        :disabled="defaultDisableCondition()"
                        @click="fetch0(0, 1)"
                        ><span v-if="fetching"><b-spinner small /></span>
                        <span v-else>Load</span>
                      </b-button>
                    </b-col>
                    <b-col class="my-auto">
                      <b-button
                        variant="primary"
                        block
                        :pressed="showEntriesWithErrors"
                        :disabled="defaultDisableCondition()"
                        @click="toggleShowEntriesWithErrors"
                        >Entries with errors
                      </b-button>
                    </b-col>
                    <b-col class="my-auto">
                      <b-button
                        variant="primary"
                        block
                        :pressed="!hideEntriesWithNoTags"
                        :disabled="defaultDisableCondition()"
                        @click="toggleHideEntriesWithNoTags"
                        >Entries with no tags to add
                      </b-button>
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
                          @change="pageClicked0"
                        />
                      </div>
                    </template>
                  </b-row>
                  <b-table-simple
                    v-if="showTable0 && browseMode === 0"
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
                      <b-th class="col-9 align-middle">Videos</b-th>
                    </b-thead>
                    <b-tbody
                      v-if="videos.filter(video => video.visible).length > 0"
                    >
                      <tr
                        v-for="video in videos.filter(vid => vid.visible)"
                        :key="video.song.id"
                      >
                        <td>
                          <div v-if="video.thumbnailsOk.length > 0">
                            <b-form-checkbox
                              v-if="video.tagsToAssign.length > 0"
                              v-model="video.toAssign"
                              size="lg"
                              :disabled="defaultDisableCondition()"
                            ></b-form-checkbox>
                          </div>
                        </td>
                        <td>
                          <b-link
                            target="_blank"
                            :to="getEntryUrl(video.song)"
                            v-html="video.song.name"
                          />
                          <b-link target="_blank" :to="getEntryUrl(video.song)">
                            <b-badge
                              class="badge text-center ml-2"
                              :variant="getSongTypeVariant(video.song.songType)"
                            >
                              {{ getSongType(video.song.songType) }}
                            </b-badge>
                            {{ video.song.createDate }}
                          </b-link>
                          <div class="text-muted">
                            {{ video.song.artistString }}
                          </div>
                        </td>
                        <td>
                          <b-row
                            v-for="(
                              thumbnail, thumbnail_key
                            ) in video.thumbnailsOk"
                            :key="thumbnail_key"
                          >
                            <b-col class="col-8">
                              <b-button
                                :disabled="defaultDisableCondition()"
                                size="sm"
                                variant="primary-outline"
                                class="mr-2"
                                @click="
                                  thumbnail.expanded = !thumbnail.expanded
                                "
                              >
                                <font-awesome-icon icon="fas fa-play" />
                              </b-button>
                              <b-link
                                target="_blank"
                                :to="getVideoUrl(thumbnail.thumbnail.id)"
                                v-html="thumbnail.thumbnail.title"
                              />
                              <div>
                                <b-badge
                                  v-for="(nico_tag, key) in thumbnail.nicoTags"
                                  :key="key"
                                  v-clipboard:copy="nico_tag.name"
                                  class="m-sm-1"
                                  href="#"
                                  :variant="nico_tag.variant"
                                  >{{ nico_tag.name }}
                                  <font-awesome-icon
                                    v-if="nico_tag.locked"
                                    icon="fas fa-lock"
                                    class="ml-1"
                                  />
                                </b-badge>
                              </div>
                              <b-collapse
                                :id="getCollapseId(thumbnail.thumbnail.id)"
                                :visible="thumbnail.expanded && !fetching"
                                class="mt-2 collapsed"
                              >
                                <b-card
                                  v-cloak
                                  :id="'embed_' + thumbnail.thumbnail.id"
                                  class="embed-responsive embed-responsive-16by9"
                                >
                                  <iframe
                                    v-if="thumbnail.expanded && !fetching"
                                    class="embed-responsive-item"
                                    allowfullscreen="allowfullscreen"
                                    style="border: none"
                                    :src="getEmbedAddr(thumbnail.thumbnail.id)"
                                  ></iframe>
                                </b-card>
                              </b-collapse>
                            </b-col>
                            <b-col>
                              <span
                                v-for="(tag, tag_key) in thumbnail.mappedTags"
                                :key="tag_key"
                              >
                                <b-button
                                  size="sm"
                                  class="m-1"
                                  :disabled="
                                    tag.assigned || defaultDisableCondition()
                                  "
                                  :variant="
                                    getTagVariant(tag, video.tagsToAssign)
                                  "
                                  @click="toggleTagAssignation(tag, video)"
                                >
                                  <font-awesome-icon
                                    :icon="getTagIcon(tag, video.tagsToAssign)"
                                    class="sm mr-sm-1"
                                  />
                                  {{ tag.tag.name }}
                                </b-button>
                              </span>
                              <div
                                v-if="thumbnail.mappedTags.length === 0"
                                class="text-muted"
                              >
                                No mapped tags available for this video
                              </div>
                            </b-col>
                          </b-row>
                          <b-row
                            v-for="(
                              thumbnail, thumbnail_err_key
                            ) in video.thumbnailsErr"
                            :key="thumbnail_err_key"
                          >
                            <b-col>
                              <b-link
                                :to="getDeletedVideoAddr(thumbnail.id)"
                                target="_blank"
                                >{{ thumbnail.title }}
                              </b-link>
                              <span
                                ><b-badge
                                  variant="danger"
                                  size="sm"
                                  class="ml-1"
                                  >{{ thumbnail.code }}</b-badge
                                ></span
                              >
                              <div>
                                <span
                                  ><b-badge
                                    v-if="thumbnail.code === 'DELETED'"
                                    variant="warning"
                                    class="m-1"
                                  >
                                    Needs to be disabled</b-badge
                                  >
                                  <b-badge
                                    v-else-if="thumbnail.code === 'COMMUNITY'"
                                    variant="warning"
                                    class="m-1"
                                  >
                                    Needs to be tagged with
                                    <b-link
                                      target="_blank"
                                      :to="'https://vocadb.net/T/7446/niconico-community-exclusive'"
                                      >niconico community exclusive</b-link
                                    ></b-badge
                                  >
                                  <b-badge v-else variant="warning" class="m-1">
                                    Unknown error</b-badge
                                  >
                                </span>
                              </div>
                            </b-col>
                          </b-row>
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
                          @change="pageClicked0"
                        />
                      </div>
                    </template>
                  </b-row>
                  <b-toast
                    id="error"
                    title="Error"
                    no-auto-hide
                    variant="danger"
                    class="m-0 rounded-0"
                    toaster="toaster-2"
                  >
                    {{ alertMessage }}
                  </b-toast>
                </b-row>
              </b-tab>
              <b-tab title="Browse by date and time">
                <b-row>
                  <div
                    class="py-lg-3 px-lg-4 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
                  >
                    <b-row>
                      <b-col class="my-auto">
                        <b-dropdown
                          :disabled="defaultDisableCondition()"
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
                          :disabled="defaultDisableCondition() || showTable1"
                          :text="getSortingCondition()"
                          variant="primary"
                        >
                          <b-dropdown-item
                            v-for="(key, value) in sortOptions"
                            :key="key"
                            :disabled="sortBy === value"
                            @click="setSortBy(value)"
                          >
                            {{ sortOptions[value] }}
                          </b-dropdown-item>
                        </b-dropdown>
                      </b-col>
                      <b-col class="m-auto">
                        <b-button
                          variant="primary"
                          block
                          :disabled="
                            defaultDisableCondition() ||
                            dateBound === '' ||
                            !dateIsValid
                          "
                          @click="
                            fetch1('right', {
                              mode: addedMode,
                              createDate: dateBound + 'T00:00:00',
                              sortRule: sortBy,
                              reverse: false
                            })
                          "
                          ><span v-if="fetching"><b-spinner small /></span>
                          <span v-else>
                            <span v-if="videos.length === 0">Load</span>
                            <span v-else>Reload</span>
                          </span>
                        </b-button>
                      </b-col>
                      <b-col class="my-auto">
                        <b-button
                          variant="primary"
                          block
                          :pressed="showEntriesWithErrors"
                          :disabled="defaultDisableCondition()"
                          @click="toggleShowEntriesWithErrors"
                          >Entries with errors
                        </b-button>
                      </b-col>
                      <b-col class="my-auto">
                        <b-button
                          variant="primary"
                          block
                          :pressed="!hideEntriesWithNoTags"
                          :disabled="defaultDisableCondition()"
                          @click="toggleHideEntriesWithNoTags"
                          >Entries with no tags to add
                        </b-button>
                      </b-col>
                    </b-row>
                    <b-row class="mt-3">
                      <b-col class="my-auto">
                        <template>
                          <b-input-group inline>
                            <b-form-input
                              v-model="dateBound"
                              type="text"
                              placeholder="Select reference date"
                              :state="validateState()"
                            />

                            <template #prepend>
                              <b-form-datepicker
                                v-model="dateBound"
                                :max="now"
                                :min="min"
                                :initial-date="now"
                                locale="en"
                                button-only
                                :disabled="fetching"
                                hide-header
                                style="width: 80px"
                              />
                            </template>
                            <template #append>
                              <b-button
                                style="width: 80px"
                                variant="danger"
                                :disabled="dateBound === '' || fetching"
                                @click="clearBoundData"
                              >
                                Clear
                              </b-button>
                            </template>
                          </b-input-group>
                        </template>
                      </b-col>
                      <b-col class="my-auto text-left align-middle">
                        <b-dropdown
                          block
                          :disabled="defaultDisableCondition()"
                          :text="getAddedModeCondition()"
                          variant="primary"
                        >
                          <b-dropdown-item
                            v-for="(key, value) in addedModeOptions"
                            :key="key"
                            :disabled="addedMode === value"
                            @click="setAdditionMode(value)"
                          >
                            {{ addedModeOptions[value] }}
                          </b-dropdown-item>
                        </b-dropdown>
                      </b-col>
                    </b-row>
                  </div>
                  <b-row v-if="showTable1" class="col-12">
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
                  <b-row v-if="showTable1" class="flex-fill text-center">
                    <div class="overflow-auto mx-auto flex-fill my-3">
                      <b-button-group class="col-2">
                        <b-button
                          variant="link"
                          :disabled="defaultDisableCondition()"
                          class="text-left pl-3"
                          @click="
                            fetch1(
                              'left',
                              leftButtonPayload(
                                sortBy,
                                addedMode,
                                videos[0].song.createDate
                              )
                            )
                          "
                          ><font-awesome-icon
                            icon="fa-solid fa-less-than"
                            class="mr-3"
                          />{{ sortBy === "CreateDate" ? "Older" : "Newer" }}
                        </b-button>
                        <b-button
                          variant="link"
                          :disabled="defaultDisableCondition()"
                          class="text-right pr-3"
                          @click="
                            fetch1(
                              'right',
                              rightButtonPayload(
                                sortBy,
                                videos[videos.length - 1].song.createDate
                              )
                            )
                          "
                          >{{ sortBy === "CreateDate" ? "Newer" : "Older"
                          }}<font-awesome-icon
                            icon="fa-solid fa-greater-than"
                            class="ml-3"
                          />
                        </b-button>
                      </b-button-group>
                    </div>
                  </b-row>
                  <b-table-simple
                    v-if="showTable1 && browseMode === 1"
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
                      <b-th class="col-9 align-middle">Videos</b-th>
                    </b-thead>
                    <b-tbody
                      v-if="videos.filter(video => video.visible).length > 0"
                    >
                      <tr
                        v-for="video in videos.filter(vid => vid.visible)"
                        :key="video.song.id"
                      >
                        <td>
                          <div v-if="video.thumbnailsOk.length > 0">
                            <b-form-checkbox
                              v-if="video.tagsToAssign.length > 0"
                              v-model="video.toAssign"
                              size="lg"
                              :disabled="defaultDisableCondition()"
                            ></b-form-checkbox>
                          </div>
                        </td>
                        <td>
                          <b-link
                            target="_blank"
                            :to="getEntryUrl(video.song)"
                            v-html="video.song.name"
                          />
                          <b-link target="_blank" :to="getEntryUrl(video.song)">
                            <b-badge
                              class="badge text-center ml-2"
                              :variant="getSongTypeVariant(video.song.songType)"
                            >
                              {{ getSongType(video.song.songType) }}
                            </b-badge>
                          </b-link>
                          <b-badge
                            v-clipboard:copy="
                              video.song.createDate.split('T')[0]
                            "
                            class="m-sm-1"
                            href="#"
                            variant="light"
                            ><font-awesome-icon
                              icon="fa-solid fa-calendar"
                              class="mr-1"
                            />{{ video.song.createDate.split("T")[0] }}
                          </b-badge>
                          <div class="text-muted">
                            {{ video.song.artistString }}
                          </div>
                        </td>
                        <td>
                          <b-row
                            v-for="(
                              thumbnail, thumbnail_key
                            ) in video.thumbnailsOk"
                            :key="thumbnail_key"
                          >
                            <b-col class="col-8">
                              <b-button
                                :disabled="defaultDisableCondition()"
                                size="sm"
                                variant="primary-outline"
                                class="mr-2"
                                @click="
                                  thumbnail.expanded = !thumbnail.expanded
                                "
                              >
                                <font-awesome-icon icon="fas fa-play" />
                              </b-button>
                              <b-link
                                target="_blank"
                                :to="getVideoUrl(thumbnail.thumbnail.id)"
                                v-html="thumbnail.thumbnail.title"
                              />
                              <div>
                                <b-badge
                                  v-for="(nico_tag, key) in thumbnail.nicoTags"
                                  :key="key"
                                  v-clipboard:copy="nico_tag.name"
                                  class="m-sm-1"
                                  href="#"
                                  :variant="nico_tag.variant"
                                  >{{ nico_tag.name }}
                                  <font-awesome-icon
                                    v-if="nico_tag.locked"
                                    icon="fas fa-lock"
                                    class="ml-1"
                                  />
                                </b-badge>
                              </div>
                              <b-collapse
                                :id="getCollapseId(thumbnail.thumbnail.id)"
                                :visible="thumbnail.expanded && !fetching"
                                class="mt-2 collapsed"
                              >
                                <b-card
                                  v-cloak
                                  :id="'embed_' + thumbnail.thumbnail.id"
                                  class="embed-responsive embed-responsive-16by9"
                                >
                                  <iframe
                                    v-if="thumbnail.expanded && !fetching"
                                    class="embed-responsive-item"
                                    allowfullscreen="allowfullscreen"
                                    style="border: none"
                                    :src="getEmbedAddr(thumbnail.thumbnail.id)"
                                  ></iframe>
                                </b-card>
                              </b-collapse>
                            </b-col>
                            <b-col>
                              <span
                                v-for="(tag, tag_key) in thumbnail.mappedTags"
                                :key="tag_key"
                              >
                                <b-button
                                  size="sm"
                                  class="m-1"
                                  :disabled="
                                    tag.assigned || defaultDisableCondition()
                                  "
                                  :variant="
                                    getTagVariant(tag, video.tagsToAssign)
                                  "
                                  @click="toggleTagAssignation(tag, video)"
                                >
                                  <font-awesome-icon
                                    :icon="getTagIcon(tag, video.tagsToAssign)"
                                    class="sm mr-sm-1"
                                  />
                                  {{ tag.tag.name }}
                                </b-button>
                              </span>
                              <div
                                v-if="thumbnail.mappedTags.length === 0"
                                class="text-muted"
                              >
                                No mapped tags available for this video
                              </div>
                            </b-col>
                          </b-row>
                          <b-row
                            v-for="(
                              thumbnail, thumbnail_err_key
                            ) in video.thumbnailsErr"
                            :key="thumbnail_err_key"
                          >
                            <b-col>
                              <b-link
                                :to="getDeletedVideoAddr(thumbnail.id)"
                                target="_blank"
                                >{{ thumbnail.title }}
                              </b-link>
                              <span
                                ><b-badge
                                  variant="danger"
                                  size="sm"
                                  class="ml-1"
                                  >{{ thumbnail.code }}</b-badge
                                ></span
                              >
                              <div>
                                <span
                                  ><b-badge
                                    v-if="thumbnail.code === 'DELETED'"
                                    variant="warning"
                                    class="m-1"
                                  >
                                    Needs to be disabled</b-badge
                                  >
                                  <b-badge
                                    v-else-if="thumbnail.code === 'COMMUNITY'"
                                    variant="warning"
                                    class="m-1"
                                  >
                                    Needs to be tagged with
                                    <b-link
                                      target="_blank"
                                      :to="'https://vocadb.net/T/7446/niconico-community-exclusive'"
                                      >niconico community exclusive</b-link
                                    ></b-badge
                                  >
                                  <b-badge v-else variant="warning" class="m-1">
                                    Unknown error</b-badge
                                  >
                                </span>
                              </div>
                            </b-col>
                          </b-row>
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
                    v-if="showTable1"
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
                  <b-row v-if="showTable1" class="flex-fill text-center mb-3">
                    <div class="overflow-auto mx-auto flex-fill my-3">
                      <b-button-group class="col-2">
                        <b-button
                          variant="link"
                          :disabled="defaultDisableCondition()"
                          class="text-left pl-3"
                          @click="
                            fetch1(
                              'left',
                              leftButtonPayload(
                                sortBy,
                                addedMode,
                                videos[0].song.createDate
                              )
                            )
                          "
                          ><font-awesome-icon
                            icon="fa-solid fa-less-than"
                            class="mr-3"
                          />{{ sortBy === "CreateDate" ? "Older" : "Newer" }}
                        </b-button>
                        <b-button
                          variant="link"
                          :disabled="defaultDisableCondition()"
                          class="text-right pr-3"
                          @click="
                            fetch1(
                              'right',
                              rightButtonPayload(
                                sortBy,
                                videos[videos.length - 1].song.createDate
                              )
                            )
                          "
                          >{{ sortBy === "CreateDate" ? "Newer" : "Older"
                          }}<font-awesome-icon
                            icon="fa-solid fa-greater-than"
                            class="ml-3"
                          />
                        </b-button>
                      </b-button-group>
                    </div>
                  </b-row>
                  <b-toast
                    id="error"
                    title="Error"
                    no-auto-hide
                    variant="danger"
                    class="m-0 rounded-0"
                    toaster="toaster-2"
                  >
                    {{ alertMessage }}
                  </b-toast>
                </b-row>
              </b-tab>
            </b-tabs>
          </b-container>
        </div>
      </b-col>
    </b-row>
    <b-row class="fixed-top m-1" style="z-index: 1; max-width: min-content">
      <b-col class="p-0">
        <b-link to="nicovideo" target="_blank">
          <b-button size="sm" style="width: 60px" variant="dark" squared
            >Toggle<br />mode
          </b-button>
        </b-link>
      </b-col>
    </b-row>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";
import {
  MappedTag,
  MinimalTag,
  NicoVideoWithError,
  NicoVideoWithMappedTags,
  SongForApiContractSimplified
} from "@/backend/dto";
import { api } from "@/backend";

import VueClipboard from "vue-clipboard2";
import { DateTime } from "luxon";

Vue.use(VueClipboard);

@Component({ components: {} })
export default class extends Vue {
  private dateIsValid = false;
  private browseMode = 0;
  private showTable0 = false;
  private showTable1 = false;
  private now = new Date();
  private min = new Date("2012-02-23T09:58:03");
  private dateBound = "";
  private dateTimeBoundStringBefore = "";
  private dateTimeBoundStringSince = "";
  private orderBy = "AdditionDate";
  private orderOptions = {
    PublishDate: "upload time",
    AdditionDate: "addition time",
    RatingScore: "user rating"
  };
  private sortBy = "CreateDate";
  private sortOptions = {
    CreateDate: "old→new",
    CreateDateDescending: "new→old"
  };
  private addedMode = "since";
  private addedModeOptions = {
    before: "before",
    since: "since"
  };
  private startOffset: number = 0;
  private maxResults: number = 10;
  private songTypes: SongType[] = [
    { name: "Unspecified", show: true },
    { name: "Original", show: true },
    { name: "Remaster", show: true },
    { name: "Remix", show: true },
    { name: "Cover", show: true },
    { name: "Instrumental", show: true },
    { name: "Mashup", show: true },
    { name: "MusicPV", show: true },
    { name: "DramaPV", show: true },
    { name: "Other", show: true }
  ];
  private videos: EntryWithVideosAndVisibility[] = [];
  private totalVideoCount: number = 0;
  private fetching: boolean = false;
  private page: number = 1;
  private numOfPages: number = 1;
  private massAssigning: boolean = false;
  private alertMessage: string = "";
  private pageToJump: number = this.page;
  private maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
  private hideEntriesWithNoTags: boolean = false;
  private showEntriesWithErrors: boolean = true;
  private songTypeToTag = {
    Unspecified: [],
    Original: [6479],
    Remaster: [1519, 391, 371],
    Remix: [371, 74, 391],
    Cover: [74, 371, 392],
    Instrumental: [208],
    MusicPV: [7378, 74, 4582],
    Mashup: [3392],
    DramaPV: [
      104,
      1736,
      7276,
      3180,
      7728,
      8509,
      7748,
      7275,
      6701,
      3186,
      8130,
      6700,
      7615,
      6703,
      6702,
      7988,
      6650,
      8043,
      8409
    ],
    Other: []
  };
  private distinct_song_count: number = 0;

  async fetch0(newStartOffset: number, newPage: number): Promise<void> {
    this.fetching = true;
    try {
      let videos: EntryWithVideosAndVisibility[] = [];
      this.distinct_song_count = 0;
      let end = false;
      let response;
      while (this.distinct_song_count < this.maxResults && !end) {
        response = await api.fetchVideosFromDb({
          startOffset: newStartOffset + this.distinct_song_count,
          maxResults: 10,
          orderBy: this.orderBy
        });
        end = response.items.length < 10;
        let videos_temp = response.items.map(entry => {
          return {
            song: entry.song,
            toAssign: false,
            visible: true,
            thumbnailsOk: entry.thumbnailsOk.map(t => {
              return {
                thumbnail: t.thumbnail,
                mappedTags: t.mappedTags.map(tag => {
                  return {
                    tag: tag.tag,
                    assigned: tag.assigned,
                    toAssign: false
                  };
                }),
                expanded: false,
                nicoTags: t.nicoTags
              };
            }),
            thumbnailsErr: entry.thumbnailsErr,
            tagsToAssign: []
          };
        });

        if (this.distinct_song_count > 0) {
          const overlap = videos_temp.find(
            video => video.song.id == videos[videos.length - 1].song.id
          );
          if (overlap != undefined) {
            videos_temp.splice(0, videos_temp.indexOf(overlap) + 1);
          }
        }
        this.distinct_song_count += videos_temp.length;
        videos = videos.concat(videos_temp);

        this.totalVideoCount = response.totalCount;
      }
      videos.splice(this.maxResults);
      this.videos = videos;
      this.postProcessVideos();
      this.filterVideos();
      this.distinct_song_count = this.maxResults;
      this.page = newStartOffset / this.maxResults + 1;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.startOffset = newStartOffset;
      this.showTable0 = true;
      this.showTable1 = false;
    } catch (err) {
      this.$bvToast.show("error");
      if (err.response == undefined) {
        this.alertMessage = err.message;
      } else {
        this.alertMessage = err.response.data.message;
      }
    } finally {
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.fetching = false;
      this.pageToJump = newPage;
      this.page = newPage;
    }
  }

  async fetch1(direction: string, payload: Fetch1Payload): Promise<void> {
    this.fetching = true;
    const reverse = payload.reverse;
    try {
      let videos: EntryWithVideosAndVisibility[] = [];
      this.distinct_song_count = 0;
      let end = false;
      while (this.distinct_song_count < this.maxResults && !end) {
        let response = await api.fetchVideosFromDbBeforeSince({
          maxResults: 10,
          mode: payload.mode,
          dateTime: payload.createDate,
          sortRule: payload.sortRule
        });
        let videos_temp: EntryWithVideosAndVisibility[] = response.items.map(
          entry => {
            return {
              song: entry.song,
              toAssign: false,
              visible: true,
              thumbnailsOk: entry.thumbnailsOk.map(t => {
                return {
                  thumbnail: t.thumbnail,
                  mappedTags: t.mappedTags.map(tag => {
                    return {
                      tag: tag.tag,
                      assigned: tag.assigned,
                      toAssign: false
                    };
                  }),
                  expanded: false,
                  nicoTags: t.nicoTags
                };
              }),
              thumbnailsErr: entry.thumbnailsErr,
              tagsToAssign: []
            };
          }
        );

        if (this.distinct_song_count > 0) {
          const overlap = videos_temp.find(
            video => video.song.id == videos[videos.length - 1].song.id
          );
          if (overlap != undefined) {
            videos_temp.splice(0, videos_temp.indexOf(overlap) + 1);
          }
        }

        this.totalVideoCount = response.totalCount;

        end = videos_temp.length == 0;

        this.distinct_song_count += videos_temp.length;

        videos = videos.concat(videos_temp);

        if (this.sortBy === "CreateDate") {
          payload =
            direction == "right"
              ? this.rightButtonPayload(
                  payload.sortRule,
                  videos_temp[videos_temp.length - 1].song.createDate
                )
              : this.leftButtonPayload(
                  this.sortBy,
                  this.addedMode,
                  videos_temp[0].song.createDate
                );
        } else {
          payload =
            direction == "right"
              ? this.rightButtonPayload(
                  payload.sortRule,
                  videos_temp[videos_temp.length - 1].song.createDate
                )
              : this.leftButtonPayload(
                  this.sortBy,
                  this.addedMode,
                  this.sortBy === "CreateDate"
                    ? videos_temp[0].song.createDate
                    : videos_temp[videos_temp.length - 1].song.createDate
                );
        }
      }
      videos.splice(this.maxResults);
      if (reverse) {
        videos = videos.reverse();
      }
      this.videos = videos;
      this.postProcessVideos();
      this.filterVideos();
      this.distinct_song_count = 100;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.showTable1 = true;
      this.showTable0 = false;
    } catch (err) {
      this.$bvToast.show("error");
      if (err.response == undefined) {
        this.alertMessage = err.message;
      } else {
        this.alertMessage = err.response.data.message;
      }
    } finally {
      this.maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
      this.fetching = false;
    }
  }

  getEntryUrl(songEntry: SongForApiContractSimplified): string {
    return "https://vocadb.net/S/" + songEntry.id;
  }

  getVideoUrl(video: string): string {
    return "https://nicovideo.jp/watch/" + video;
  }

  getResultNumberStr(): string {
    return "Entries per page: " + this.maxResults;
  }

  pageClicked0(pgnum: number): void {
    this.fetch0((pgnum - 1) * this.maxResults, pgnum);
  }

  private refreshPage0() {
    this.fetch0((this.pageToJump - 1) * this.maxResults, this.pageToJump);
  }

  private rightButtonPayload(
    sortBy: string,
    createDate: string
  ): Fetch1Payload {
    if (sortBy === "CreateDate") {
      return {
        mode: "since",
        createDate: createDate,
        sortRule: "CreateDate",
        reverse: false
      };
    } else {
      return {
        mode: "before",
        createDate: createDate,
        sortRule: "CreateDateDescending",
        reverse: false
      };
    }
  }

  private leftButtonPayload(
    sortBy: string,
    mode: string,
    createDate: string
  ): Fetch1Payload {
    if (sortBy === "CreateDate") {
      return {
        mode: "before",
        createDate: createDate,
        sortRule: "CreateDateDescending",
        reverse: true
      };
    } else {
      return {
        mode: "since",
        createDate: createDate,
        sortRule: "CreateDate",
        reverse: true
      };
    }
  }

  setMaxResults(mxres: number): void {
    this.maxResults = mxres;
  }

  private async assignMultiple(): Promise<void> {
    this.massAssigning = true;
    try {
      for (const song of this.videos.filter(s => s.toAssign)) {
        await api.lookUpAndAssignTag({
          tags: song.tagsToAssign,
          songId: song.song.id
        });
        song.toAssign = false;
        const assigned_ids = song.tagsToAssign.map(tta => tta.id);
        for (const thumbnailOk of song.thumbnailsOk) {
          thumbnailOk.mappedTags.forEach(t => {
            if (assigned_ids.find(id => id == t.tag.id) != undefined) {
              t.toAssign = false;
              t.assigned = true;
            }
          });
        }
        song.tagsToAssign.splice(0, song.tagsToAssign.length);
      }
    } finally {
      this.massAssigning = false;
    }
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

  getDeletedVideoAddr(videoId: string): string {
    return "https://nicolog.jp/watch/" + videoId;
  }

  countChecked(): number {
    return this.videos.filter(video => video.toAssign).length;
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

  private setOrderBy(value: string): void {
    this.orderBy = value;
  }

  private setSortBy(value: string): void {
    this.sortBy = value;
  }

  private setAdditionMode(value: string): void {
    this.addedMode = value;
  }

  private filterVideos(): void {
    for (const video of this.videos) {
      let assignable_mapped_tags_cnt = 0;
      for (const thumbnailOk of video.thumbnailsOk) {
        assignable_mapped_tags_cnt += thumbnailOk.mappedTags.filter(
          tag => !tag.assigned
        ).length;
      }

      video.visible =
        ((this.hiddenTypes() == 0 ||
          !this.songTypes
            .filter(t => !t.show)
            .map(t => t.name)
            .includes(video.song.songType)) &&
          (!this.hideEntriesWithNoTags || assignable_mapped_tags_cnt > 0)) ||
        (this.showEntriesWithErrors && video.thumbnailsErr.length > 0);
    }
  }

  pageState(): boolean {
    return this.pageToJump > 0 && this.pageToJump <= this.maxPage;
  }

  private hiddenTypes(): number {
    return this.songTypes.filter(t => !t.show).length;
  }

  private getOrderingCondition(): string {
    return "Arrange by: " + this.orderOptions[this.orderBy];
  }

  private getSortingCondition(): string {
    return "Sorting: " + this.sortOptions[this.sortBy];
  }

  private getAddedModeCondition(): string {
    return "Mode: added " + this.addedModeOptions[this.addedMode];
  }

  private defaultDisableCondition(): boolean {
    return this.fetching || this.massAssigning;
  }

  private getTypeInfo(type: string): string {
    return (
      type +
      " (" +
      this.videos.filter(vid => vid.song != null && vid.song.songType == type)
        .length +
      ")"
    );
  }

  private getTagVariant(tag: MappedTag, tagsToAssign: MinimalTag[]): string {
    if (tag.assigned) {
      return "success";
    } else if (tagsToAssign.find(t => t.id == tag.tag.id) != undefined) {
      return "warning";
    } else {
      return "outline-success";
    }
  }

  private getTagIcon(tag: MappedTag, tagsToAssign: MinimalTag[]): string[] {
    if (tag.assigned) {
      return ["fas", "fa-check"];
    } else if (tagsToAssign.find(t => t.id == tag.tag.id) != undefined) {
      return ["fas", "fa-minus"];
    } else {
      return ["fas", "fa-plus"];
    }
  }

  private toggleTagAssignation(
    tag: MappedTag,
    video: EntryWithVideosAndVisibility
  ) {
    const assign =
      video.tagsToAssign.filter(t => t.id == tag.tag.id).length > 0;
    if (assign) {
      video.tagsToAssign = video.tagsToAssign.filter(t => t.id != tag.tag.id);
    } else {
      video.tagsToAssign.push(tag.tag);
    }
    for (const thumbnailOk of video.thumbnailsOk) {
      for (const mappedTag of thumbnailOk.mappedTags) {
        if (mappedTag.tag.id == tag.tag.id) {
          mappedTag.toAssign = !assign;
        }
      }
      video.toAssign = video.tagsToAssign.length > 0;
    }
  }

  private toggleShowEntriesWithErrors() {
    this.showEntriesWithErrors = !this.showEntriesWithErrors;
    this.filterVideos();
  }

  private toggleHideEntriesWithNoTags() {
    this.hideEntriesWithNoTags = !this.hideEntriesWithNoTags;
    this.filterVideos();
  }

  private postProcessVideos() {
    for (const video of this.videos) {
      for (const thumbnailOk of video.thumbnailsOk) {
        thumbnailOk.mappedTags = thumbnailOk.mappedTags.filter(
          t =>
            this.songTypeToTag[video.song.songType].find(
              (id: number) => id == t.tag.id
            ) == undefined
        );
        thumbnailOk.mappedTags = thumbnailOk.mappedTags.filter(function (
          elem,
          index,
          self
        ) {
          return (
            index ===
            self.indexOf(self.find(el => el.tag.name == elem.tag.name)!)
          );
        });
      }
    }
  }

  private clearBoundData() {
    this.dateBound = "";
    this.dateTimeBoundStringSince = "";
    this.dateTimeBoundStringBefore = "";
    this.videos = [];
    this.showTable1 = false;
  }

  private getDateString() {
    if (this.dateTimeBoundStringBefore != "") {
      return "Before " + this.dateTimeBoundStringBefore;
    } else if (this.dateTimeBoundStringSince != "") {
      return "Since " + this.dateTimeBoundStringSince;
    } else if (this.dateBound != "") {
      return "Before " + this.dateBound;
    } else {
      return "";
    }
  }

  private getDate(mode: string): string {
    if (mode === "before") {
      if (this.sortBy == "CreateDate") {
        return this.videos[0].song.createDate;
      } else {
        return this.videos[this.videos.length - 1].song.createDate;
      }
    } else {
      if (this.sortBy == "CreateDate") {
        return this.videos[0].song.createDate;
      } else {
        return this.videos[this.videos.length - 1].song.createDate;
      }
    }
  }

  private generateFetch1Payload(
    direction: string,
    sortBy: string,
    videos: EntryWithVideosAndVisibility[]
  ): Fetch1Payload {
    if (direction === "right") {
      if (sortBy === "CreateDate") {
        return {
          mode: "since",
          createDate: videos[videos.length - 1].song.createDate,
          sortRule: "CreateDate",
          reverse: false
        };
      } else {
        return {
          mode: "before",
          createDate: videos[videos.length - 1].song.createDate,
          sortRule: "CreateDateDescending",
          reverse: false
        };
      }
    } else {
      if (sortBy === "CreateDate") {
        return {
          mode: "since",
          createDate: videos[0].song.createDate,
          sortRule: "CreateDateDescending",
          reverse: true
        };
      } else {
        return {
          mode: "before",
          createDate: videos[0].song.createDate,
          sortRule: "CreateDate",
          reverse: true
        };
      }
    }
  }

  private validateState(): boolean | null {
    if (this.dateBound === "") {
      return null;
    } else if (!DateTime.fromSQL(this.dateBound).isValid) {
      this.dateIsValid = false;
      return false;
    }
    try {
      let d = new Date(this.dateBound);
      if (d.getFullYear() < this.min.getFullYear()) {
        this.dateIsValid = false;
        return false;
      } else if (d.getFullYear() === this.min.getFullYear()) {
        if (d.getMonth() < this.min.getMonth()) {
          this.dateIsValid = false;
          return false;
        } else if (
          d.getMonth() === this.min.getMonth() &&
          d.getDate() < this.min.getDate()
        ) {
          this.dateIsValid = false;
          return false;
        }
      }
      if (d.getFullYear() > this.now.getFullYear()) {
        this.dateIsValid = false;
        return false;
      } else if (d.getFullYear() === this.now.getFullYear()) {
        if (d.getMonth() > this.now.getMonth()) {
          this.dateIsValid = false;
          return false;
        } else if (d.getMonth() === this.now.getMonth()) {
          if (d.getDate() > this.now.getDate()) {
            this.dateIsValid = false;
            return false;
          }
        }
      }
      this.dateIsValid = true;
      return true;
    } catch (e) {
      this.dateIsValid = false;
      return false;
    }
  }
}

export interface Fetch1Payload {
  mode: string;
  createDate: string;
  sortRule: string;
  reverse: boolean;
}

export interface EntryWithVideosAndVisibility {
  thumbnailsOk: NicoVideoWithMappedTags[];
  thumbnailsErr: NicoVideoWithError[];
  song: SongForApiContractSimplified;
  visible: boolean;
  toAssign: boolean;
  tagsToAssign: MinimalTag[];
}

export interface SongType {
  name: string;
  show: boolean;
}
</script>
