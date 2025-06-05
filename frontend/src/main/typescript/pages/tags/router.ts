import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";
import { serviceName } from "@/constants";
import { BrowseMode, defaultMode } from "@/pages/tags/utils";

Vue.use(VueRouter);

const routes: RouteConfig[] = [
  {
    path: "/:browseMode/:targName",
    name: "tags-full",
    props: true,
    component: () => import("@/pages/tags/views/Tags.vue")
  },
  {
    path: "/:browseMode",
    name: "tags-mode",
    props: true,
    component: () => import("@/pages/tags/views/Tags.vue")
  },
  {
    path: "/",
    name: "tags",
    props: {
      browseMode: defaultMode
    },
    component: () => import("@/pages/tags/views/Tags.vue")
  }
];

export const router = new VueRouter({
  base: "/tags",
  mode: "history",
  routes: routes
});

router.beforeEach((to, from, next) => {
  const mode: BrowseMode = (to.params["browseMode"] as BrowseMode) ?? defaultMode;
  const tagName = to.params["targName"] != undefined ? `𖤘 ${to.params["targName"]}` : "Tags";
  switch (mode) {
    case "song-entries":
      document.title = `${serviceName} | Tags on song entries`;
      break;
    case "vocadb":
      document.title = `${serviceName} | ${tagName} (VocaDB)`;
      break;
    case "nicovideo":
      document.title = `${serviceName} | ${tagName} (NND)`;
      break;
    default:
      document.title = `${serviceName} | ${tagName} (${mode})`;
      break;
  }
  next();
});
