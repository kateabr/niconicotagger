<template>
  <div
    v-if="databaseOptions.length > 0"
    style="display: flex; align-items: center"
    class="min-vh-100 min-vw-100"
  >
    <div class="col-lg-3 mx-auto text-center">
      <h1>NicoNicoTagger 2.0</h1>
      <div class="blockquote">for VocaDB</div>
      <div class="container">
        <b-form class="justify-content-center" @submit="login">
          <b-form-group label-for="loginBox">
            <b-form-input
              id="loginBox"
              v-model="username"
              placeholder="Username"
              required
              @keydown.enter.native="login"
            ></b-form-input>
          </b-form-group>

          <b-form-group label-for="passwordBox">
            <b-form-input
              id="passwordBox"
              v-model="password"
              placeholder="Password"
              type="password"
              required
              @keydown.enter.native="login"
            />
          </b-form-group>

          <b-form-group label-for="dbDropdown">
            <b-row class="px-2">
              <b-col class="text-left my-auto">
                <span>Database:</span>
              </b-col>
              <b-col>
                <b-dropdown
                  id="dbDropdown"
                  :disabled="loggingIn"
                  block
                  :text="selectedDatabase.displayName"
                  class="my-auto"
                  variant="link"
                  menu-class="w-100"
                >
                  <b-dropdown-item
                    v-for="dbOption in databaseOptions"
                    :key="dbOption.clientType"
                    @click="selectedDatabase = dbOption"
                    >{{ dbOption.displayName }}
                  </b-dropdown-item>
                </b-dropdown>
              </b-col>
            </b-row>
          </b-form-group>
          <b-button
            :disabled="loggingIn || username.length < 1 || password.length < 1"
            block
            variant="primary"
            @click="login"
            >Log in</b-button
          >
        </b-form>
      </div>
      <b-toaster class="b-toaster-bottom-center" name="toaster"> </b-toaster>
    </div>
    <nav-bar-menu
      style="position: fixed; top: 0"
      class="flex-lg-nowrap col-12"
      active-mode="index"
      :client-type="clientType"
    />
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";
import { api } from "@/backend";
import NavBarMenu from "@/components/NavBarMenu.vue";
import { getErrorData, getClientType } from "@/utils";
import {
  localStorageKeyBaseUrl,
  localStorageKeyClientType,
  unknownClientType
} from "@/constants";
import { AxiosError } from "axios";
import { SupportedDatabaseResponse } from "@/backend/dto/response/supportedDatabaseResponse";

@Component({ components: { NavBarMenu } })
export default class extends Vue {
  private username: string = "";
  private password: string = "";
  private loggingIn: boolean = false;
  private clientType: string = "";
  private databaseOptions: SupportedDatabaseResponse[] = [];
  private selectedDatabase: SupportedDatabaseResponse = {
    clientType: "",
    displayName: "",
    baseAddress: ""
  };

  private async login() {
    this.loggingIn = true;
    try {
      await api.authorize({
        userName: this.username,
        password: this.password,
        clientType: this.selectedDatabase.clientType
      });
      this.$bvToast.toast("Logged in to " + this.selectedDatabase.displayName, {
        title: "Success",
        toaster: "b-toaster-bottom-center",
        solid: true,
        variant: "success",
        noAutoHide: true
      });
    } catch (err) {
      this.clientType = unknownClientType;
      localStorage.removeItem(localStorageKeyClientType);
      const errorData = getErrorData((err as AxiosError).response);
      this.$bvToast.toast(errorData.message, {
        title: errorData.statusText,
        toaster: "b-toaster-bottom-center",
        solid: true,
        variant: "danger",
        noAutoHide: true
      });
    } finally {
      this.loggingIn = false;
      localStorage.setItem(
        localStorageKeyClientType,
        this.selectedDatabase.clientType
      );
      localStorage.setItem(
        localStorageKeyBaseUrl,
        this.selectedDatabase.baseAddress
      );
    }
    this.clientType = this.selectedDatabase.clientType;
  }

  private async loadSupportedDatabases() {
    this.databaseOptions = await api.getSupportedDatabases();
    this.selectedDatabase = {
      clientType: this.clientType,
      displayName: this.databaseOptions.filter(
        entry => entry.clientType == this.clientType
      )[0].displayName,
      baseAddress: this.databaseOptions.filter(
        entry => entry.clientType == this.clientType
      )[0].baseAddress
    };
  }

  created(): void {
    let clientType = getClientType();
    if (clientType != unknownClientType) {
      this.clientType = clientType;
    } else {
      this.clientType = "vocadb";
    }
    this.loadSupportedDatabases();
  }
}
</script>
