/*
  Freeze Table Header
    Make table headers always visible in the viewport.
    http://brentmuir.com/projects/freezeheader

  Copyright (c) 2011 Brent Muir

  Licensed under the MIT license (http://www.opensource.org/licenses/mit-license)

  Version 0.6.1 (with modifications for Project Buendia)

  Usage:
    $("table").freezeHeader();
    $("table").freezeHeader({ top: true, left: false });
        - This will create frozen THEAD headers for every table on the page.
        - This is the default.
    $("table").freezeHeader({ top: false, left: true });
        - This will create frozen left column headers for every table on the page.
    $("table").freezeHeader({ top: true, left: false });
        - This will create frozen THEAD and left column headers for every table on the page.
        - This will also freeze the top left corner of the table.

  Assumptions:
    - Table will have THEAD and TBODY tags to distinguish the header from the rest of the table.
    - TH elements are used in the THEAD.
    - Assumes a relatively simple table with no merged cells.

  Limitations:
    - Setting a left margin on the table itself breaks the calculations below. Wrap the
      table in a DIV and set the margins on the DIV instead.
    - Not pixel-perfect in IE

*/

(function ($) {
    var frameRequested = false;

    var methods = {
        'init': function (options) {
            var settings = {'top': true, 'left': false};
            if (options) { $.extend(settings, options); }

            return this.each(function () {
                var $this = $(this);
                var data = $this.data('freezeHeader');

                // If the plugin hasn't been initialized yet
                if (! data) {
                    var topHeader = null;
                    var leftHeader = null;
                    var cornerHeader = null;

                    // add divs within TH elements to force width
                    $this.find("th").wrapInner("<div>");

                    // add divs within all other TD elements
                    $this.find("td").wrapInner("<div>");

                    // To create a frozen top header, we clone the entire table and remove the TBODY
                    // Need to wrap the table in a div because dynamically setting position:fixed on a table
                    // doesn't work in IE8, but setting it on the div does.
                    // Set initial div positioning to overlap existing table to work around IE8 bug (otherwise
                    // document height will include the cloned tables even though they are moved later)
                    if (settings.left) {
                        leftHeader = $this.clone(false)
                            .find("th:nth-child(n+2), td:nth-child(n+2)").remove().end()
                            .appendTo(document.body).wrap("<div>").parent()
                            .css({position: 'absolute',
                                  top: $this.offset().top,
                                  left: $this.offset().left});
                    }

                    if (settings.top) {
                        topHeader = $this.clone(false)
                            .children("tbody").remove().end()
                            .appendTo(document.body)
                            .wrap("<div>").parent()
                            .css({position: 'fixed', visibility: 'hidden'});
                    }

                    if (settings.left && settings.top) {
                        cornerHeader = topHeader.clone(false) // skip a few steps by cloning topHeader
                            .find("th:nth-child(n+2)").remove().end()
                            .appendTo(document.body)
                            .css({position: 'fixed', visibility: 'hidden'});
                    }

                    $this.data('freezeHeader', {top: topHeader, left: leftHeader, corner: cornerHeader});
                }
                $(window).bind('resize.freezeHeader', {table: $this}, methods.resize);
                $(window).bind('scroll.freezeHeader', {table: $this}, methods.scroll);
                $('#grid-scroller').bind('scroll.freezeHeader', {table: $this}, methods.scroll);
                $(window).trigger('resize'); // force a resize event to calculate all widths/heights
            });
        }, // end init()

        'destroy': function () {
            return this.each(function () {
                var $this = $(this);
                var data = $this.data('freezeHeader');

                $(window).unbind('resize.freezeHeader');
                $(window).unbind('scroll.freezeHeader');

                data.top.remove();
                data.left.remove();
                data.corner.remove();

                $this.removeData('freezeHeader');
            })
        },

        'resize': function (event) {
            var table = event.data.table;
            var topHeader = table.data('freezeHeader').top;
            var leftHeader = table.data('freezeHeader').left;
            var cornerHeader = table.data('freezeHeader').corner;

            // set the width of the header th elements to the same as the data table th elements
            if (topHeader) {
                topHeader.find("th>div").each(function (i) {
                    if (typeof window.getComputedStyle == 'function') {
                        $(this).width(window.getComputedStyle(table.find("th>div").eq(i).get(0),"").getPropertyValue("width"));
                    } else {
                        $(this).width(table.find("th>div").eq(i).width());
                    }
                });
            }

            // set the width and height of the corner
            if (cornerHeader) {
                cornerHeader.find("th>div").width(table.find("th").eq(0).width());
                cornerHeader.find("th>div").height(table.find("th").eq(0).height());
            }

            // set the width and height of the frozen column headers
            if (leftHeader) {
                $(leftHeader.children().attr("rows")).each(function (i) {
                    var tdDivs = $(table.attr("rows")[i]).children().children();
                    var maxHeight = 0;
                    var height = 0;
                    var width = 0;
                    // the divs in a row might have varying heights, so find the largest one
                    // use getComputedStyle where we can because FF uses fractional heights and
                    // height() only returns whole pixels
                    tdDivs.each(function () {
                        if (typeof window.getComputedStyle == 'function') {
                            height = window.getComputedStyle(this,"").getPropertyValue("height").replace("px", "");
                        } else {
                            height = $(this).height();
                        }
                        maxHeight = Math.max(maxHeight, height);
                    });
                    $(this).children().children().eq(0).height(maxHeight + "px"); // need to add px to get FF to recognise fraction

                    if (typeof window.getComputedStyle == 'function') {
                        width = window.getComputedStyle(tdDivs.eq(0).get(0),"").getPropertyValue("width").replace("px", "");
                    } else {
                        width = tdDivs.first().width();
                    }
                    // need to add px to get FF to recognise fraction
                    $(this).children().children().eq(0).width((parseFloat(width)) + "px");
                });
            }

            var metrics = {
                tableTop: table.offset().top,
                tableLeft: table.offset().left,
                tableHeight: table.height(),
                topHeaderHeight: topHeader ? topHeader.height() : 0,
                tableWidth: table.width(),
                leftHeaderWidth: leftHeader ? leftHeader.width() : 0
            };
            metrics.tableBottom = metrics.tableTop + metrics.tableHeight - metrics.topHeaderHeight;
            metrics.tableRight = metrics.tableLeft + metrics.tableWidth - metrics.leftHeaderWidth;
            table.data('freezeHeaderMetrics', metrics);

            // trigger a scroll event because a resize may reposition some elements
            $(window).trigger('scroll');
        }, // end resize()

        'scroll': function (event) {
            if (!frameRequested) {
                frameRequested = true;
                window.requestAnimationFrame(function() {
                    methods.update(event.data.table);
                });
            }
        },

        'update': function (table) {
            var freezeHeader = table.data('freezeHeader');
            var metrics = table.data('freezeHeaderMetrics');
            var topHeader = freezeHeader.top;
            var leftHeader = freezeHeader.left;
            var cornerHeader = freezeHeader.corner;
            var scrollTop = $(window).scrollTop();
            var scrollLeft = $(window).scrollLeft() + $('#grid-scroller').scrollLeft();

            // To avoid flickering, use position fixed and hide whenever we can
            if (topHeader) {
                var headerTop = Math.min(Math.max(0, metrics.tableTop - scrollTop),
                                         metrics.tableBottom - scrollTop);
                topHeader.css(
                    (scrollTop < metrics.tableTop) ?
                        {visibility: 'hidden'} :
                        {visibility: 'visible',
                         position: 'fixed',
                         top: headerTop,
                         left: metrics.tableLeft - scrollLeft});
            }

            var headerLeft = Math.min(Math.max(0, metrics.tableLeft - scrollLeft),
                                      metrics.tableRight - scrollLeft);
            if (cornerHeader) {
                cornerHeader.css(
                    (scrollTop < metrics.tableTop || scrollLeft < metrics.tableLeft) ?
                        {visibility: 'hidden'} :
                        {visibility: 'visible',
                         position: 'fixed',
                         top: headerTop,
                         left: headerLeft});
            }

            frameRequested = false;
        }
    };

    $.fn.freezeHeader = function (method) {
        if (methods[method]) {
            return methods[method].apply(this, Array.prototype.slice.call(arguments, 1));
        } else if (typeof method === 'object' || ! method) {
            return methods.init.apply(this, arguments);
        } else {
            $.error('Method ' +  method + ' does not exist on jQuery.freezeHeader');
        }
    };

})(jQuery);
