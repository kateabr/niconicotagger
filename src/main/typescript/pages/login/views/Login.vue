<template>
  <div style="display: flex; align-items: center" class="min-vh-100 min-vw-100">
    <nav-bar-menu
      style="position: fixed; top: 0"
      class="flex-lg-nowrap col-12"
      active-mode="index"
      :db-address="dbAddress"
    />
    <div class="col-lg-3 mx-auto text-center">
      <h1>NicoNicoTagger</h1>
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
                  :text="databaseOptions[database]"
                  class="my-auto"
                  variant="link"
                  menu-class="w-100"
                >
                  <b-dropdown-item
                    v-for="(key, value) in databaseOptions"
                    :key="key"
                    @click="database = value"
                    >{{ key }}
                  </b-dropdown-item>
                </b-dropdown>
              </b-col>
            </b-row>
          </b-form-group>
          <b-button :disabled="loggingIn" block variant="primary" @click="login"
            >Log in</b-button
          >
          <b-toast
            id="error"
            title="Error"
            no-auto-hide
            variant="danger"
            class="m-0 rounded-0"
            toaster="toaster"
          >
            {{ alertMessage }}
          </b-toast>
          <b-toast
            id="success"
            no-auto-hide
            variant="success"
            class="m-0 rounded-0"
            toaster="toaster"
          >
            Successfully logged in!
          </b-toast>
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

@Component({ components: { NavBarMenu } })
export default class extends Vue {
  private username: string = "";
  private password: string = "";
  private loggingIn: boolean = false;
  private alertMessage: string = "";
  private database: string = "VocaDb";
  private databaseOptions = { VocaDb: "VocaDB", VocaDbBeta: "VocaDB BETA" };
  private databaseAddressOptions = {
    VocaDb: "https://vocadb.net",
    VocaDbBeta: "https://beta.vocadb.net"
  };
  private dbAddress: string = "";

  private async login() {
    this.loggingIn = true;
    try {
      let response = await api.authenticate({
        username: this.username,
        password: this.password,
        database: this.database
      });
      localStorage.setItem("accessToken", response.data.token);
      this.$bvToast.show("success");
    } catch (err) {
      this.$bvToast.show("error");
      this.alertMessage = err.response.data.message;
    } finally {
      this.loggingIn = false;
      localStorage.setItem(
        "dbAddress",
        this.databaseAddressOptions[this.database]
      );
    }
  }

  created(): void {
    let dbAddress = localStorage.getItem("dbAddress");
    if (this.dbAddress == "" && dbAddress != null) {
      this.dbAddress = dbAddress;
    }
  }
}
</script>
