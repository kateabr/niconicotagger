import Vue from "vue";
import VueRouter, { RouteConfig } from "vue-router";
import { serviceName } from "@/constants";

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
      browseMode: "song-entries"
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
  const mode = to.params["browseMode"];
  if (mode == undefined || mode == "song-entries") {
    document.title = `${serviceName} | Tags on song entries`;
    next();
    return;
  }
  const tagName = to.params["targName"] != undefined ? `𖤘 ${to.params["targName"]}` : "Tags";
  switch (mode) {
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
