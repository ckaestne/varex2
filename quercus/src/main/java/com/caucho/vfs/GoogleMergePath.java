/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Nam Nguyen
 */

package com.caucho.vfs;

import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;

import java.util.ArrayList;

/**
 * Represents the root merged path for Google.
 */
public class GoogleMergePath extends MergePath
{
  private GooglePath _googlePath;

  public GoogleMergePath(Path pwd, String bucket, boolean isGooglePathPrimary)
  {
    String namespace = "quercus_" + bucket;

    FileService fileService = FileServiceFactory.getFileService();
    GoogleInodeService inodeService = new GoogleInodeService(namespace);

    GoogleStorePath gsPath
      = new GoogleStorePath(fileService, inodeService, bucket);

    gsPath.init();

    _googlePath = gsPath;

    if (isGooglePathPrimary) {
      addMergePath(gsPath);
      addMergePath(pwd);
    }
    else {
      addMergePath(pwd);
      addMergePath(gsPath);
    }
  }

  private GoogleMergePath(GoogleMergePath mergePath)
  {
    _googlePath = mergePath._googlePath;

    ArrayList<Path> list = mergePath.getPathList();

    for (Path path : list) {
      addMergePath(path.copy());
    }
  }

  public GooglePath getGooglePath()
  {
    return _googlePath;
  }

  @Override
  public Path copy()
  {
    GoogleMergePath copy = new GoogleMergePath(this);

    return copy;
  }
}
