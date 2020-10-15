package com.icecreamqaq.yuq.util

import com.IceCreamQAQ.Yu.util.Web
import com.icecreamqaq.yuq.yuq

class WebHelper {
    companion object{
        fun Web.getWithQQKey(url: String) = this.get(convertUrl(url))
        fun Web.postWithQQKey(url: String, para: MutableMap<String, String>): String {
            for ((k, v) in para) {
                para[k] = when (v) {
                    "{gtk}" -> yuq.cookieEx.gtk.toString()
                    "{skey}" -> yuq.cookieEx.skey
                    "{psgtk}" -> {
                        val domain = url.split("://")[1].split("/")[0]
                        var psgtk = ""
                        for ((k, v) in yuq.cookieEx.pskeyMap) {
                            if (domain.endsWith(k)) {
                                psgtk = v.gtk.toString()
                                break
                            }
                        }
                        psgtk
                    }
                    else -> v
                }
            }
            return this.post(convertUrl(url), para)
        }

        fun convertUrl(url: String): String {
            var u = url.replace("{gtk}", yuq.cookieEx.gtk.toString(), true)
            u = u.replace("{skey}", yuq.cookieEx.skey, true)
            if (u.contains("{psgtk}", true)) {
                val domain = u.split("://")[1].split("/")[0]
                for ((k, v) in yuq.cookieEx.pskeyMap) {
                    if (domain.endsWith(k)) {
                        u = u.replace("{psgtk}", v.gtk.toString(), true)
                        break
                    }
                }
            }
            return u
        }
    }
}