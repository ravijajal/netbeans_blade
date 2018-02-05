/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.modules.php.blade.editor.resolver;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.MIMEResolver;

public class BladeMIMEResolver extends MIMEResolver {

    private final String mime = "text/x-blade";
    private final String ext = ".blade.php";

    public BladeMIMEResolver() {
        this("text/x-blade");
    }

    public BladeMIMEResolver(String... mimeTypes) {
        super(mimeTypes);
    }

    @Override
    public String findMIMEType(FileObject fo) {
        if (fo.getNameExt().toLowerCase().endsWith(ext)) {
            return mime;
        }
        return null;
    }
}
