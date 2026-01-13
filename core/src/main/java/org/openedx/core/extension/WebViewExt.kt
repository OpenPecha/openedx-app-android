package org.openedx.core.extension

import android.webkit.WebView

/**
 * Injects CSS to hide common headers and footers in web pages.
 *
 * Also removes padding/margins from body to eliminate empty space.
 */
fun WebView.injectHeaderFooterHidingCss() {
    val css = """
        /* Hide common site headers/footers */
        header, footer,
        [role="banner"], [role="contentinfo"],
        .site-header, .site-footer,
        .global-header, .global-footer,
        .header, .footer,
        .navbar-fixed-top, .navbar, .topbar,
        .bottom-bar, .cookie-banner, .gdpr-banner,
        .certificate .wrapper-banner.wrapper-banner-user,
        #header, #footer, #masthead, #site-footer, #site-header {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            min-height: 0 !important;
            max-height: 0 !important;
            margin: 0 !important;
            padding: 0 !important;
            border: 0 !important;
        }

        /* Remove empty space created by fixed headers */
        body {
            padding-top: 0 !important;
            padding-bottom: 0 !important;
            margin-top: 0 !important;
            margin-bottom: 0 !important;
        }
    """

    val js = """
        (function() {
            var style = document.createElement('style');
            style.type = 'text/css';
            style.appendChild(document.createTextNode(`$css`));
            document.head.appendChild(style);
        })();
    """.trimIndent()

    evaluateJavascript(js, null)
}
