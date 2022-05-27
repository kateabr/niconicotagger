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
        {{ alertMessage }}
      </b-toast>
      <b-tabs v-model="browseMode" class="mt-3" content-class="mt-3">
        <b-tab title="Browse by NicoNicoDouga tag" active>
          <b-row>
            <span class="m-auto col-lg-5">
              <b-input-group inline class="mt-lg-3">
                <template #prepend>
                  <b-button
                    v-b-toggle.scope-collapse
                    variant="primary"
                    style="width: 80px"
                    :disabled="defaultDisableCondition()"
                    ><font-awesome-icon
                      class="mr-sm-1"
                      icon="fas fa-angle-down"
                    />More</b-button
                  >
                </template>
                <b-form-input
                  id="tag-form"
                  v-model.trim="tag"
                  :disabled="defaultDisableCondition()"
                  placeholder="NicoNicoDouga tag"
                  @keydown.enter.native="initPageClicked"
                >
                </b-form-input>
                <template #append>
                  <b-button
                    v-if="!fetching"
                    variant="primary"
                    style="width: 80px"
                    :disabled="tag === '' || defaultDisableCondition()"
                    @click="initPageClicked"
                    >Load</b-button
                  >
                  <b-button
                    v-else
                    variant="primary"
                    style="width: 80px"
                    disabled
                    ><b-spinner small></b-spinner
                  ></b-button>
                </template>
              </b-input-group>
              <b-collapse
                id="scope-collapse"
                v-model="showCollapse"
                class="mt-2"
              >
                <b-row>
                  <b-col>
                    <b-input-group inline>
                      <b-form-input
                        id="scope-tag-form"
                        v-model="scopeTag"
                        placeholder="Specify tag scope (NND)"
                        :disabled="defaultDisableCondition()"
                        @keydown.enter.native="initPageClicked"
                      >
                      </b-form-input>
                      <template #prepend>
                        <b-button
                          variant="secondary"
                          style="width: 80px"
                          @click="scopeTag = getDefaultScopeTag()"
                        >
                          <font-awesome-icon icon="fa-solid fa-paste" />
                        </b-button>
                      </template>
                      <template #append>
                        <b-button
                          variant="danger"
                          style="width: 80px"
                          :disabled="scopeTag === ''"
                          @click="scopeTag = ''"
                          >Clear</b-button
                        >
                      </template>
                    </b-input-group>
                  </b-col>
                </b-row>
                <b-row v-if="tagFrozen !== '' && activeMode(0)" class="mt-2">
                  <b-col>
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
                  <b-col>
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
                            :state="pageState()"
                            @keydown.enter.native="
                              pageState() ? pageClicked(pageToJump) : null
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
                            @click="pageClicked(pageToJump)"
                            ><span v-if="pageToJump === page">Refresh</span
                            ><span v-else>Jump</span></b-button
                          >
                        </template>
                      </b-input-group>
                    </template>
                  </b-col>
                </b-row>
                <b-row v-if="tagFrozen !== '' && activeMode(0)" class="mt-2">
                  <b-col>
                    <b-form-checkbox
                      v-model="showVideosWithUploaderEntry"
                      @change="filter(false)"
                    >
                      Force show videos whose uploaders have entries at VocaDB
                    </b-form-checkbox>
                  </b-col>
                </b-row>
              </b-collapse>
            </span>
          </b-row>
          <b-row
            v-if="notEmptyTagInfo() && activeMode(0)"
            class="mt-lg-3 pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
          >
            <b-col class="my-auto"
              >Tag:<br /><strong>
                <a
                  v-if="notEmptyTagInfo()"
                  target="_blank"
                  :href="getNicoTag(tagFrozen, scopeTagFrozen !== '')"
                  >{{ tagFrozen
                  }}<font-awesome-icon class="ml-1" icon="fas fa-external-link"
                /></a> </strong
            ></b-col>
            <b-col class="my-auto"
              >Mapped to:<br />
              <strong v-for="(tag, key) in tagMappings" :key="key">
                <a target="_blank" :href="getVocaDBTag(tagInfo, key)">{{
                  tag
                }}</a
                ><span v-if="tagMappings.length - key > 1">, </span>
              </strong>
            </b-col>
            <b-col class="my-auto"
              >Videos found:<br /><strong>{{ totalVideoCount }}</strong></b-col
            >
            <b-col class="my-auto">
              <b-dropdown
                block
                :disabled="defaultDisableCondition()"
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
              <b-button
                :disabled="defaultDisableCondition()"
                variant="primary"
                block
                :pressed.sync="noEntry"
                @click="filter(false)"
                >Videos without entries
              </b-button>
            </b-col>
            <b-col class="my-auto">
              <b-button
                :disabled="defaultDisableCondition()"
                variant="primary"
                block
                :pressed.sync="tagged"
                @click="filter(false)"
                >Tagged songs
              </b-button>
            </b-col>
          </b-row>
          <b-row v-if="notEmptyTagInfo() && activeMode(0)">
            <b-col class="col-12">
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
                      filter(false);
                    "
                    >{{ getTypeInfo(type.name) }}
                  </b-button>
                </b-button-group>
              </div>
            </b-col>
          </b-row>
          <b-row v-if="notEmptyTagInfo() && activeMode(0)">
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
                ></b-pagination>
              </div>
            </template>
          </b-row>
          <b-row v-if="notEmptyTagInfo() && activeMode(0)">
            <b-table-simple hover class="mt-1 col-lg-12">
              <b-thead>
                <b-th>
                  <b-form-checkbox
                    v-model="allChecked"
                    size="lg"
                    :disabled="
                      defaultDisableCondition() || noVideosWithEntries()
                    "
                    @change="checkAll"
                  ></b-form-checkbox>
                </b-th>
                <b-th class="col-8 align-middle">Video</b-th>
                <b-th class="col-3 align-middle">Entry</b-th>
                <b-th class="col-1 align-middle">Tag</b-th>
              </b-thead>
              <b-tbody v-if="!allInvisible(videosToDisplay0)">
                <b-tr
                  v-for="(value, key) in videosToDisplay0"
                  :id="value.video.contentId"
                  :key="key"
                  :style="value.rowVisible ? '' : 'display: none'"
                >
                  <b-td>
                    <div
                      v-if="
                        value.songEntry != null && !value.songEntry.tagInTags
                      "
                    >
                      <b-form-checkbox
                        v-model="value.toAssign"
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
                      @click="value.embedVisible = !value.embedVisible"
                    >
                      <font-awesome-icon icon="fas fa-play" />
                    </b-button>
                    <a
                      target="_blank"
                      :href="getVideoUrl(value.video)"
                      v-html="value.video.title"
                    ></a>
                    <div>
                      <b-badge
                        v-for="(value1, key1) in value.video.tags"
                        :key="key1"
                        v-clipboard:copy="value1.name"
                        class="m-sm-1"
                        :variant="value1.variant"
                        href="#"
                      >
                        <font-awesome-icon icon="fas fa-tag" class="mr-1" />
                        {{ value1.name }}
                      </b-badge>
                    </div>
                    <b-collapse
                      :id="getCollapseId(value.video.contentId)"
                      :visible="value.embedVisible && !fetching"
                      class="mt-2 collapsed"
                    >
                      <b-card
                        v-cloak
                        :id="'embed_' + value.video.contentId"
                        class="embed-responsive embed-responsive-16by9"
                      >
                        <iframe
                          v-if="value.embedVisible && !fetching"
                          class="embed-responsive-item"
                          allowfullscreen="allowfullscreen"
                          style="border: none"
                          :src="getEmbedAddr(value.video.contentId)"
                        ></iframe>
                      </b-card>
                    </b-collapse>
                  </b-td>
                  <b-td>
                    <div v-if="value.songEntry != null">
                      <a
                        target="_blank"
                        :href="getEntryUrl(value.songEntry)"
                        v-html="value.songEntry.name"
                      ></a>
                      <a target="_blank" :href="getEntryUrl(value.songEntry)">
                        <b-badge
                          class="badge text-center ml-2"
                          :variant="
                            getSongTypeVariant(value.songEntry.songType)
                          "
                        >
                          {{ getSongType(value.songEntry.songType) }}
                        </b-badge>
                      </a>
                      <div class="text-muted">
                        {{ value.songEntry.artistString }}
                      </div>
                    </div>
                    <div v-else>
                      <b-button
                        size="sm"
                        :disabled="fetching"
                        :href="getAddSongUrl(value.video.contentId)"
                        target="_blank"
                        >Add to the database
                      </b-button>
                      <div
                        v-if="value.publisher !== null"
                        class="small text-secondary"
                      >
                        Published by
                        <a
                          target="_blank"
                          :href="getArtistUrl(value.publisher)"
                          >{{ value.publisher.name.displayName }}</a
                        >
                      </div>
                    </div>
                  </b-td>
                  <b-td>
                    <div v-if="value.songEntry != null">
                      <b-button-toolbar key-nav>
                        <b-button
                          v-if="value.songEntry.tagInTags"
                          style="pointer-events: none"
                          class="btn disabled"
                          variant="success"
                        >
                          <font-awesome-icon icon="fas fa-check" />
                        </b-button>
                        <b-button
                          v-else
                          :id="getButtonId(value.songEntry)"
                          :disabled="defaultDisableCondition()"
                          class="btn"
                          variant="outline-success"
                          @click="assign(value.songEntry.id)"
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
                    :disabled="
                      defaultDisableCondition() || noVideosWithEntries()
                    "
                    @change="checkAll"
                  ></b-form-checkbox>
                </b-th>
                <b-th class="col-8 align-middle">Video</b-th>
                <b-th class="col-3 align-middle">Entry</b-th>
                <b-th class="col-1 align-middle">Tag</b-th>
              </b-tfoot>
            </b-table-simple>
          </b-row>
          <b-row
            v-if="notEmptyTagInfo() && activeMode(0)"
            class="mt-lg-1 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
          >
            <b-col class="col-lg-3 m-auto">
              <b-button
                block
                variant="primary"
                :disabled="countChecked() === 0 || massAssigning || fetching"
                @click="assignMultiple"
              >
                <div v-if="massAssigning">
                  <b-spinner small class="mr-1"></b-spinner>
                  Assigning...
                </div>
                <div v-else>Batch assign ({{ countChecked() }} selected)</div>
              </b-button>
            </b-col>
          </b-row>
          <b-row v-if="notEmptyTagInfo() && activeMode(0)">
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
                @change="pageClicked"
              ></b-pagination>
            </div>
          </b-row>
        </b-tab>
        <b-tab title="Browse by VocaDB tag">
          <b-row>
            <span class="m-auto col-lg-5">
              <b-input-group inline class="mt-lg-3">
                <template #prepend>
                  <b-button
                    v-b-toggle.scope-collapse
                    variant="primary"
                    style="width: 80px"
                    :disabled="defaultDisableCondition()"
                    ><font-awesome-icon
                      class="mr-sm-1"
                      icon="fas fa-angle-down"
                    />More</b-button
                  >
                </template>
                <b-form-input
                  id="tag-form"
                  v-model.trim="tag"
                  :disabled="defaultDisableCondition()"
                  placeholder="VocaDB tag"
                  @keydown.enter.native="initPageClicked"
                >
                </b-form-input>
                <template #append>
                  <b-button
                    v-if="!fetching"
                    variant="primary"
                    style="width: 80px"
                    :disabled="tag === '' || defaultDisableCondition()"
                    @click="initPageClicked"
                    >Load</b-button
                  >
                  <b-button
                    v-else
                    variant="primary"
                    style="width: 80px"
                    disabled
                    ><b-spinner small></b-spinner
                  ></b-button>
                </template>
              </b-input-group>
              <b-collapse
                id="scope-collapse"
                v-model="showCollapse"
                class="mt-2"
              >
                <b-row>
                  <b-col>
                    <b-input-group inline>
                      <b-form-input
                        id="scope-tag-form"
                        v-model="scopeTag"
                        placeholder="Specify tag scope (NND)"
                        :disabled="defaultDisableCondition()"
                        @keydown.enter.native="initPageClicked"
                      >
                      </b-form-input>
                      <template #prepend>
                        <b-button
                          variant="secondary"
                          style="width: 80px"
                          @click="scopeTag = getDefaultScopeTag()"
                        >
                          <font-awesome-icon icon="fa-solid fa-paste" />
                        </b-button>
                      </template>
                      <template #append>
                        <b-button
                          variant="danger"
                          style="width: 80px"
                          :disabled="scopeTag === ''"
                          @click="scopeTag = ''"
                          >Clear</b-button
                        >
                      </template>
                    </b-input-group>
                  </b-col>
                </b-row>
                <b-row v-if="tagFrozen !== '' && activeMode(1)" class="mt-2">
                  <b-col>
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
                  <b-col>
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
                            :state="pageState()"
                            @keydown.enter.native="
                              pageState() ? pageClicked(pageToJump) : null
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
                            @click="pageClicked(pageToJump)"
                            ><span v-if="pageToJump === page">Refresh</span
                            ><span v-else>Jump</span></b-button
                          >
                        </template>
                      </b-input-group>
                    </template>
                  </b-col>
                </b-row>
                <b-row v-if="tagFrozen !== '' && activeMode(1)" class="mt-2">
                  <b-col>
                    <b-form-checkbox
                      v-model="showVideosWithUploaderEntry"
                      @change="filter(false)"
                    >
                      Force show videos whose uploaders have entries at VocaDB
                    </b-form-checkbox>
                  </b-col>
                </b-row>
              </b-collapse>
            </span>
          </b-row>
          <b-row
            v-if="notEmptyTagInfo() && activeMode(1)"
            class="mt-lg-3 pt-lg-3 pb-lg-3 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
          >
            <b-col class="my-auto"
              >Tag:<br /><strong>
                <a
                  v-if="notEmptyTagInfo()"
                  target="_blank"
                  :href="getVocaDBTag(tagInfo, 0)"
                  >{{ tagFrozen }}</a
                >
              </strong></b-col
            >
            <b-col class="my-auto"
              >Search expression:<br /><strong>
                <a
                  v-if="notEmptyTagInfo()"
                  target="_blank"
                  :href="
                    getNicoTag(tagMappings.join(' OR '), scopeTagFrozen !== '')
                  "
                  >view at NND<font-awesome-icon
                    class="ml-1"
                    icon="fas fa-external-link" /></a></strong
            ></b-col>
            <b-col class="my-auto"
              >Videos found:<br /><strong>{{ totalVideoCount }}</strong></b-col
            >
            <b-col class="my-auto">
              <b-dropdown
                block
                :disabled="defaultDisableCondition()"
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
              <b-button
                :disabled="defaultDisableCondition()"
                variant="primary"
                block
                :pressed.sync="noEntry"
                @click="filter(false)"
                >Videos without entries
              </b-button>
            </b-col>
            <b-col class="my-auto">
              <b-button
                :disabled="defaultDisableCondition()"
                variant="primary"
                block
                :pressed.sync="tagged"
                @click="filter(false)"
                >Tagged songs
              </b-button>
            </b-col>
          </b-row>
          <b-row v-if="notEmptyTagInfo() && activeMode(1)">
            <b-col class="col-12">
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
                      filter(false);
                    "
                    >{{ getTypeInfo(type.name) }}
                  </b-button>
                </b-button-group>
              </div>
            </b-col>
          </b-row>
          <b-row v-if="notEmptyTagInfo() && activeMode(1)">
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
                ></b-pagination>
              </div>
            </template>
          </b-row>
          <b-row v-if="notEmptyTagInfo() && activeMode(1)">
            <b-table-simple hover class="mt-1 col-lg-12">
              <b-thead>
                <b-th>
                  <b-form-checkbox
                    v-model="allChecked"
                    size="lg"
                    :disabled="
                      defaultDisableCondition() || noVideosWithEntries()
                    "
                    @change="checkAll"
                  ></b-form-checkbox>
                </b-th>
                <b-th class="col-8 align-middle">Video</b-th>
                <b-th class="col-3 align-middle">Entry</b-th>
                <b-th class="col-1 align-middle">Tag</b-th>
              </b-thead>
              <b-tbody v-if="!allInvisible(videosToDisplay1)">
                <b-tr
                  v-for="(value, key) in videosToDisplay1"
                  :id="value.video.contentId"
                  :key="key"
                  :style="value.rowVisible ? '' : 'display: none'"
                >
                  <b-td>
                    <div
                      v-if="
                        value.songEntry != null && !value.songEntry.tagInTags
                      "
                    >
                      <b-form-checkbox
                        v-model="value.toAssign"
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
                      @click="value.embedVisible = !value.embedVisible"
                    >
                      <font-awesome-icon icon="fas fa-play" />
                    </b-button>
                    <a
                      target="_blank"
                      :href="getVideoUrl(value.video)"
                      v-html="value.video.title"
                    ></a>
                    <div>
                      <b-badge
                        v-for="(value1, key1) in value.video.tags"
                        :key="key1"
                        v-clipboard:copy="value1.name"
                        class="m-sm-1"
                        :variant="value1.variant"
                        href="#"
                      >
                        <font-awesome-icon icon="fas fa-tag" class="mr-1" />
                        {{ value1.name }}
                      </b-badge>
                    </div>
                    <b-collapse
                      :id="getCollapseId(value.video.contentId)"
                      :visible="value.embedVisible && !fetching"
                      class="mt-2 collapsed"
                    >
                      <b-card
                        v-cloak
                        :id="'embed_' + value.video.contentId"
                        class="embed-responsive embed-responsive-16by9"
                      >
                        <iframe
                          v-if="value.embedVisible && !fetching"
                          class="embed-responsive-item"
                          allowfullscreen="allowfullscreen"
                          style="border: none"
                          :src="getEmbedAddr(value.video.contentId)"
                        ></iframe>
                      </b-card>
                    </b-collapse>
                  </b-td>
                  <b-td>
                    <div v-if="value.songEntry != null">
                      <a
                        target="_blank"
                        :href="getEntryUrl(value.songEntry)"
                        v-html="value.songEntry.name"
                      ></a>
                      <a target="_blank" :href="getEntryUrl(value.songEntry)">
                        <b-badge
                          class="badge text-center ml-2"
                          :variant="
                            getSongTypeVariant(value.songEntry.songType)
                          "
                        >
                          {{ getSongType(value.songEntry.songType) }}
                        </b-badge>
                      </a>
                      <div class="text-muted">
                        {{ value.songEntry.artistString }}
                      </div>
                    </div>
                    <div v-else>
                      <b-button
                        size="sm"
                        :disabled="fetching"
                        :href="getAddSongUrl(value.video.contentId)"
                        target="_blank"
                        >Add to the database
                      </b-button>
                      <div
                        v-if="value.publisher !== null"
                        class="small text-secondary"
                      >
                        Published by
                        <a
                          target="_blank"
                          :href="getArtistUrl(value.publisher)"
                          >{{ value.publisher.name.displayName }}</a
                        >
                      </div>
                    </div>
                  </b-td>
                  <b-td>
                    <div v-if="value.songEntry != null">
                      <b-button-toolbar key-nav>
                        <b-button
                          v-if="value.songEntry.tagInTags"
                          style="pointer-events: none"
                          class="btn disabled"
                          variant="success"
                        >
                          <font-awesome-icon icon="fas fa-check" />
                        </b-button>
                        <b-button
                          v-else
                          :id="getButtonId(value.songEntry)"
                          :disabled="defaultDisableCondition()"
                          class="btn"
                          variant="outline-success"
                          @click="assign(value.songEntry.id)"
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
                    :disabled="
                      defaultDisableCondition() || noVideosWithEntries()
                    "
                    @change="checkAll"
                  ></b-form-checkbox>
                </b-th>
                <b-th class="col-8 align-middle">Video</b-th>
                <b-th class="col-3 align-middle">Entry</b-th>
                <b-th class="col-1 align-middle">Tag</b-th>
              </b-tfoot>
            </b-table-simple>
          </b-row>
          <b-row
            v-if="notEmptyTagInfo() && activeMode(1)"
            class="mt-lg-1 col-lg-12 text-center m-auto alert-primary rounded p-sm-2 bg-light progress-bar-striped"
          >
            <b-col class="col-lg-3 m-auto">
              <b-button
                block
                variant="primary"
                :disabled="countChecked() === 0 || massAssigning || fetching"
                @click="assignMultiple"
              >
                <div v-if="massAssigning">
                  <b-spinner small class="mr-1"></b-spinner>
                  Assigning...
                </div>
                <div v-else>Batch assign ({{ countChecked() }} selected)</div>
              </b-button>
            </b-col>
          </b-row>
          <b-row v-if="notEmptyTagInfo() && activeMode(1)">
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
                @change="pageClicked"
              ></b-pagination>
            </div>
          </b-row>
        </b-tab>
      </b-tabs>
    </b-container>
    <b-row class="fixed-top m-1" style="z-index: 1; max-width: min-content">
      <b-col class="p-0">
        <b-link to="vocadb" target="_blank">
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
  AssignableTag,
  NicoVideoWithTidyTags,
  Publisher,
  SongForApiContractSimplified
} from "@/backend/dto";
import { api } from "@/backend";

import VueClipboard from "vue-clipboard2";

Vue.use(VueClipboard);

@Component({ components: {} })
export default class extends Vue {
  private tag: string = "";
  private orderBy = "startTime";
  private orderOptions = {
    startTime: "upload time",
    viewCounter: "views",
    lengthSeconds: "length"
  };
  private tagFrozen: string = "";
  private tagInfo: AssignableTag[] = [];
  private startOffset: number = 0;
  private maxResults: number = 10;
  private tagMappings: string[] = [];
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
  private videos0: VideoWithEntryAndVisibility[] = [];
  private videos1: VideoWithEntryAndVisibility[] = [];
  private videosToDisplay0: VideoWithEntryAndVisibility[] = [];
  private videosToDisplay1: VideoWithEntryAndVisibility[] = [];
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
  private scopeTag: string = "";
  private scopeTagFrozen: string = "";
  private showCollapse: boolean = false;
  private pageToJump: number = this.page;
  private maxPage = Math.ceil(this.totalVideoCount / this.maxResults);
  private browseMode = 0;

  async fetch0(
    targetTag: string,
    newStartOffset: number,
    newPage: number
  ): Promise<void> {
    if (this.tag == "") {
      return;
    }
    this.showCollapse = false;
    this.fetching = true;
    try {
      let response = await api.fetchVideos({
        tag: targetTag,
        scopeTag: this.scopeTag,
        startOffset: newStartOffset,
        maxResults: this.maxResults,
        orderBy: this.orderBy
      });
      this.videos0 = response.items.map(vid => {
        return {
          video: vid.video,
          songEntry: vid.songEntry,
          embedVisible: false,
          rowVisible: true,
          toAssign: false,
          publisher: vid.publisher
        };
      });
      this.filter(true);
      this.tagMappings = response.tagMappings;
      this.totalVideoCount = response.totalVideoCount;
      this.scopeTagFrozen = response.safeScope;
      this.scopeTag = response.safeScope;
      this.tagInfo = response.tags;
      this.tagFrozen = targetTag;
      this.tag = targetTag;
      this.page = newStartOffset / this.maxResults + 1;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.startOffset = newStartOffset;
      this.allChecked = false;
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
      this.videos1 = [];
      this.videosToDisplay1 = [];
      this.videos0 = [];
    }
  }

  async fetch1(
    targetTag: string,
    newStartOffset: number,
    newPage: number
  ): Promise<void> {
    if (this.tag == "") {
      return;
    }
    this.showCollapse = false;
    this.fetching = true;
    try {
      let response = await api.fetchVideosByTag({
        tag: targetTag,
        scopeTag: this.scopeTag,
        startOffset: newStartOffset,
        maxResults: this.maxResults,
        orderBy: this.orderBy
      });
      this.videos1 = response.items.map(vid => {
        return {
          video: vid.video,
          songEntry: vid.songEntry,
          embedVisible: false,
          rowVisible: true,
          toAssign: false,
          publisher: vid.publisher
        };
      });
      this.filter(true);
      this.tagMappings = response.tagMappings;
      this.totalVideoCount = response.totalVideoCount;
      this.scopeTagFrozen = response.safeScope;
      this.scopeTag = response.safeScope;
      this.tagInfo = response.tags;
      this.tagFrozen = response.tags[0].name;
      this.tag = response.tags[0].name;
      this.page = newStartOffset / this.maxResults + 1;
      this.numOfPages = this.totalVideoCount / this.maxResults + 1;
      this.startOffset = newStartOffset;
      this.allChecked = false;
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
      this.videos0 = [];
      this.videosToDisplay0 = [];
      this.videos1 = [];
    }
  }

  getEntryUrl(songEntry: SongForApiContractSimplified): string {
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

  initPageClicked(): void {
    if (this.browseMode == 0) {
      this.fetch0(this.tag.trim(), 0, 1);
    } else if (this.browseMode == 1) {
      this.fetch1(this.tag.trim(), 0, 1);
    }
  }

  pageClicked(pgnum: number): void {
    if (this.browseMode == 0) {
      this.fetch0(this.tagFrozen, (pgnum - 1) * this.maxResults, pgnum);
    } else if (this.browseMode == 1) {
      this.fetch1(this.tagFrozen, (pgnum - 1) * this.maxResults, pgnum);
    }
  }

  setMaxResults(mxres: number): void {
    this.maxResults = mxres;
  }

  private async assign(id: number): Promise<void> {
    this.assigning = true;
    await api.assignTag({ tags: this.tagInfo, songId: id });
    let songEntry = null;
    if (this.browseMode == 0) {
      songEntry = this.videosToDisplay0.filter(video => {
        if (video.songEntry == null) return false;
        return video.songEntry.id == id;
      })[0].songEntry as SongForApiContractSimplified;
    } else if (this.browseMode == 1) {
      songEntry = this.videosToDisplay1.filter(video => {
        if (video.songEntry == null) return false;
        return video.songEntry.id == id;
      })[0].songEntry as SongForApiContractSimplified;
    }
    this.assigning = false;
    if (songEntry != null) {
      songEntry.tagInTags = true;
    }
  }

  private async assignMultiple(): Promise<void> {
    this.massAssigning = true;
    try {
      if (this.browseMode == 0) {
        for (const vid1 of this.videosToDisplay0.filter(vid => vid.toAssign)) {
          let songEntry = vid1.songEntry as SongForApiContractSimplified;
          await this.assign(songEntry.id);
          vid1.toAssign = false;
        }
      } else if (this.browseMode == 1) {
        for (const vid1 of this.videosToDisplay1.filter(vid => vid.toAssign)) {
          let songEntry = vid1.songEntry as SongForApiContractSimplified;
          await this.assign(songEntry.id);
          vid1.toAssign = false;
        }
      }
    } finally {
      this.massAssigning = false;
      this.allChecked = false;
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

  private checkAll(): void {
    if (this.browseMode == 0) {
      this.videosToDisplay0
        .filter(video => video.songEntry != null && !video.songEntry.tagInTags)
        .forEach(video => (video.toAssign = this.allChecked));
    } else if (this.browseMode == 1) {
      this.videosToDisplay1
        .filter(video => video.songEntry != null && !video.songEntry.tagInTags)
        .forEach(video => (video.toAssign = this.allChecked));
    }
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

  private filter(init: boolean): void {
    if (init) {
      if (this.browseMode == 0) {
        this.filterNested(this.videos0);
        this.videosToDisplay0 = this.videos0;
      } else if (this.browseMode == 1) {
        this.filterNested(this.videos1);
        this.videosToDisplay1 = this.videos1;
      }
    } else {
      if (this.browseMode == 0) {
        this.filterNested(this.videosToDisplay0);
      } else if (this.browseMode == 1) {
        this.filterNested(this.videosToDisplay1);
      }
    }
  }

  private filterNested(src: VideoWithEntryAndVisibility[]): void {
    const hiddenTypes = this.hiddenTypes() > 0;
    for (var i = 0; i < src.length; ++i) {
      src[i].rowVisible =
        (src[i].songEntry != null ||
          this.noEntry ||
          (src[i].publisher != null && this.showVideosWithUploaderEntry)) &&
        (src[i].songEntry == null ||
          this.tagged ||
          !src[i].songEntry?.tagInTags);
      if (hiddenTypes) {
        const songEntryTemp = src[i].songEntry;
        if (songEntryTemp != null) {
          src[i].rowVisible = !this.songTypes
            .filter(t => !t.show)
            .map(t => t.name)
            .includes(songEntryTemp.songType);
        }
      }
    }
  }

  private allInvisible(list: VideoWithEntryAndVisibility[]): boolean {
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

  private getDefaultScopeTag(): string {
    return "-歌ってみた VOCALOID OR UTAU OR CEVIO OR SYNTHV OR SYNTHESIZERV OR neutrino(歌声合成エンジン) OR DeepVocal OR Alter/Ego OR AlterEgo OR AquesTalk OR AquesTone OR AquesTone2 OR ボカロ OR ボーカロイド OR 合成音声 OR 歌唱合成 OR coefont OR coefont_studio OR VOICELOID OR VOICEROID OR ENUNU OR ソフトウェアシンガー";
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

  created(): void {
    let max_results = localStorage.getItem("max_results");
    if (max_results != null) {
      this.maxResults = parseInt(max_results);
    }
  }
}

export interface VideoWithEntryAndVisibility {
  video: NicoVideoWithTidyTags;
  songEntry: SongForApiContractSimplified | null;
  embedVisible: boolean;
  rowVisible: boolean;
  toAssign: boolean;
  publisher: Publisher | null;
}

export interface SongType {
  name: string;
  show: boolean;
}
</script>
