package com.criticollab.osgi.mavenindex.persist;/**
 * Created by ses on 2/22/17.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.Embedded;
import javax.persistence.Transient;

@Embeddable
public class VersionRange {
    @SuppressWarnings("UnusedDeclaration")
    private static Logger logger = LoggerFactory.getLogger(VersionRange.class);
    transient aQute.bnd.version.VersionRange bndVersionRange;
    private boolean leftOpen;
    private boolean rightOpen;
    private Version left = null;
    private Version right = null;

    public VersionRange() {
    }

    public VersionRange(boolean leftOpen, boolean rightOpen, Version left, Version right) {
        this.leftOpen = leftOpen;
        this.rightOpen = rightOpen;
        this.left = left;
        this.right = right;
    }

    public VersionRange(aQute.bnd.version.VersionRange bndRange) {
    }

    public boolean isLeftOpen() {
        return leftOpen;
    }

    public void setLeftOpen(boolean leftOpen) {
        this.leftOpen = leftOpen;
        bndVersionRange = null;
    }

    public boolean isRightOpen() {
        return rightOpen;
    }

    public void setRightOpen(boolean rightOpen) {
        this.rightOpen = rightOpen;
        bndVersionRange = null;
    }

    @Embedded

    public Version getLeft() {
        return left;
    }

    public void setLeft(Version left) {
        bndVersionRange = null;
        this.left = left;
    }

    @Embedded
    @AttributeOverrides({@AttributeOverride(name = "major", column = @Column(name = "rightMajor")),
                         @AttributeOverride(name = "minor", column = @Column(name = "rightMinor")),
                         @AttributeOverride(name = "micro", column = @Column(name = "rightMicro")),
                         @AttributeOverride(name = "qualifer", column = @Column(name = "rightQualifier")),})
    public Version getRight() {
        return right;
    }

    public void setRight(Version right) {
        bndVersionRange = null;
        this.right = right;
    }

    @Transient
    public aQute.bnd.version.VersionRange getBndVersionRange() {
        if (bndVersionRange == null) {
            bndVersionRange = new aQute.bnd.version.VersionRange(leftOpen, left.getVersion(), right.getVersion(),
                                                                 rightOpen);
        }
        return bndVersionRange;
    }

    public void setBndVersionRange(aQute.bnd.version.VersionRange bndVersionRange) {
        setLeft(new Version(bndVersionRange.getLow()));
        setRight(new Version(bndVersionRange.getHigh()));
        setLeftOpen(bndVersionRange.includeLow());
        setRightOpen(bndVersionRange.includeHigh());
        this.bndVersionRange = bndVersionRange;
    }
}
