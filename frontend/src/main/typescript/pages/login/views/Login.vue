<template>
  <div style="display: flex; align-items: center" class="min-vh-100 min-vw-100">
    <nav-bar-menu
      style="position: fixed; top: 0"
      class="flex-lg-nowrap col-12"
      active-mode="index"
      :client-type="clientTypeLoggedIn"
    />
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
                  :text="databaseOptions[clientType]"
                  class="my-auto"
                  variant="link"
                  menu-class="w-100"
                >
                  <b-dropdown-item
                    v-for="(key, value) in databaseOptions"
                    :key="key"
                    @click="clientType = value"
                    >{{ key }}
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
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import { Component } from "vue-property-decorator";
import { api } from "@/backend";
import NavBarMenu from "@/components/NavBarMenu.vue";
import { getErrorData, getClientType } from "@/utils";
import { ClientType } from "@/backend/dto/enumeration";
import { localStorageKeyClientType } from "@/constants";
import { AxiosError } from "axios";

@Component({ components: { NavBarMenu } })
export default class extends Vue {
  private username: string = "";
  private password: string = "";
  private loggingIn: boolean = false;
  private clientTypeLoggedIn: ClientType = getClientType();
  private clientType: ClientType =
    this.clientTypeLoggedIn != ClientType.UNKNOWN
      ? this.clientTypeLoggedIn
      : ClientType.vocadb;
  private databaseOptions = {
    [ClientType.vocadb]: "VocaDB",
    [ClientType.vocadb_beta]: "VocaDB BETA"
  };

  private async login() {
    this.loggingIn = true;
    try {
      await api.authorize({
        userName: this.username,
        password: this.password,
        clientType: ClientType[this.clientType]
      });
      this.clientTypeLoggedIn = this.clientType;
      this.$bvToast.toast(
        "Logged in to " + this.databaseOptions[this.clientTypeLoggedIn],
        {
          title: "Success",
          toaster: "b-toaster-bottom-center",
          solid: true,
          variant: "success",
          noAutoHide: true
        }
      );
    } catch (err) {
      this.clientTypeLoggedIn = ClientType.UNKNOWN;
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
      localStorage.setItem(localStorageKeyClientType, this.clientTypeLoggedIn);
    }
  }

  created(): void {
    let clientType = getClientType();
    if (clientType != ClientType.UNKNOWN) {
      this.clientTypeLoggedIn = clientType;
    }
  }
}
</script>
