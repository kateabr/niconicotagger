// eslint-disable-next-line @typescript-eslint/no-var-requires
const path = require("path");

module.exports = {
  pages: {
    events: {
      title: "NicoNicoTagger (Events)",
      entry: "src/main/typescript/pages/events/main.ts"
    },
    tags: {
      title: "NicoNicoTagger (Tags)",
      entry: "src/main/typescript/pages/tags/main.ts"
    },
    login: {
      title: "Login",
      entry: "src/main/typescript/pages/login/main.ts"
    },
    index: {
      title: "Login",
      entry: "src/main/typescript/pages/login/main.ts"
    }
  },

  configureWebpack: {
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "src/main/typescript")
      }
    }
  },

  outputDir: "target/spa",
  lintOnSave: false,

  css: {
    sourceMap: true
  },

  devServer: {
    proxy: "http://localhost:8080"
  }
};
