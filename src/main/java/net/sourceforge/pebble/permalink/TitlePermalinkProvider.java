/*
 * Copyright (c) 2003-2011, Simon Brown
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *
 *   - Neither the name of Pebble nor the names of its contributors may
 *     be used to endorse or promote products derived from this software
 *     without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package net.sourceforge.pebble.permalink;

import net.sourceforge.pebble.domain.*;

import javax.inject.Inject;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Generates permalinks based upon the blog entry title. This implementation
 * only uses the following characters from the title:
 * <ul>
 * <li>a-z</li>
 * <li>A-Z</li>
 * <li>0-9</li>
 * <li>_ (underscore)</li>
 * </ul>
 * For titles without these characters (e.g. those using an extended character
 * set) the blog entry ID is used for the permalink instead.
 *
 * @author Simon Brown
 */
public class TitlePermalinkProvider extends PermalinkProviderSupport {

  /** the regex used to check for a blog entry permalink */
  private static final String BLOG_ENTRY_PERMALINK_REGEX = "/\\d\\d\\d\\d/\\d\\d/\\d\\d/[\\w]*.html";

  @Inject
  private BlogService blogService;

  /**
   * Gets the permalink for a blog entry.
   *
   * @return  a URI as a String
   */
  public synchronized String getPermalink(BlogEntry blogEntry) {
    if (blogEntry.getTitle() == null || blogEntry.getTitle().length() == 0) {
      return buildPermalink(blogEntry) + ".html";
    } else {

      SimpleDate date = new SimpleDate(getBlog().getTimeZone(), getBlog().getLocale(), blogEntry.getDate());
      Day day = blogEntryIndex.getBlogForYear(getBlog(), date.getYear())
          .getBlogForMonth(date.getMonth()).getBlogForDay(date.getDay());
      List<String> entries = getBlog().getBlogEntryIndex().getBlogEntriesForDay(getBlog(), day.getYear(), day.getMonth(), day.getDay());
      int count = 0;
      for (int i = entries.size()-1; i > entries.indexOf(blogEntry.getId()); i--) {
        try {
          BlogEntry entry = blogService.getBlogEntry(getBlog(), entries.get(i));
          if (entry.getTitle().equals(blogEntry.getTitle())) {
            count++;
          }
        } catch (BlogServiceException e) {
          // do nothing
        }
      }

      if (count == 0) {
        return buildPermalink(blogEntry) + ".html";
      } else {
        return buildPermalink(blogEntry) + "_" + blogEntry.getId() + ".html";
      }
    }
  }

  private String buildPermalink(BlogEntry blogEntry) {
    String title = blogEntry.getTitle();
    if (title == null || title.length() == 0) {
      title = "" + blogEntry.getId();
    } else {
      title = title.toLowerCase();
      title = title.replaceAll("[\\. ,;/\\\\-]", "_");
      title = title.replaceAll("[^a-z0-9_]", "");
      title = title.replaceAll("_+", "_");
      title = title.replaceAll("^_*", "");
      title = title.replaceAll("_*$", "");
    }

    // if the title has been blanked out, use the blog entry instead
    if (title == null || title.length() == 0) {
      title = "" + blogEntry.getId();
    }

    Blog blog = blogEntry.getBlog();
    Date date = blogEntry.getDate();
    DateFormat year = new SimpleDateFormat("yyyy");
    year.setTimeZone(blog.getTimeZone());
    DateFormat month = new SimpleDateFormat("MM");
    month.setTimeZone(blog.getTimeZone());
    DateFormat day = new SimpleDateFormat("dd");
    day.setTimeZone(blog.getTimeZone());

    StringBuffer buf = new StringBuffer();
    buf.append("/");
    buf.append(year.format(date));
    buf.append("/");
    buf.append(month.format(date));
    buf.append("/");
    buf.append(day.format(date));
    buf.append("/");
    buf.append(title);

    return buf.toString();
  }

  /**
   * Determines whether the specified URI is a blog entry permalink.
   *
   * @param uri   a relative URI
   * @return      true if the URI represents a permalink to a blog entry,
   *              false otherwise
   */
  public boolean isBlogEntryPermalink(String uri) {
    if (uri != null) {
      return uri.matches(BLOG_ENTRY_PERMALINK_REGEX);
    } else {
      return false;
    }
  }

  /**
   * Gets the blog entry referred to by the specified URI.
   *
   * @param uri   a relative URI
   * @return  a BlogEntry instance, or null if one can't be found
   */
  public BlogEntry getBlogEntry(String uri) {
    Day day = getDay(uri);

    try {
      List<BlogEntry> blogEntries = blogService.getBlogEntries(getBlog(), day.getYear(), day.getMonth(), day.getDay());
      for (BlogEntry blogEntry : blogEntries) {
          // use the local permalink, just in case the entry has been aggregated
          // and an original permalink assigned
          if (blogEntry.getLocalPermalink().endsWith(uri)) {
            return blogEntry;
          }
      }
    } catch (BlogServiceException e) {
      // do nothing
    }

    return null;
  }

  void setBlogService(BlogService blogService) {
    this.blogService = blogService;
  }
}
