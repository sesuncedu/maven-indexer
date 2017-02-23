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
    private Boolean leftOpen;
    private Boolean rightOpen;
    private Version left;
    private Version right;

    public VersionRange() {
    }

    public VersionRange(String vr) {
        if (vr != null) {
            try {
                aQute.bnd.version.VersionRange versionRange = new aQute.bnd.version.VersionRange(vr);
                setBndVersionRange(versionRange);
            } catch (Exception e) {
                logger.error("Invalid version range format",
                             e); //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    public VersionRange(Boolean leftOpen, Boolean rightOpen, Version left, Version right) {
        this.leftOpen = leftOpen;
        this.rightOpen = rightOpen;
        this.left = left;
        this.right = right;
    }

    public VersionRange(aQute.bnd.version.VersionRange bndRange) {
        setBndVersionRange(bndRange);
    }

    @Column(nullable = true)
    public Boolean isLeftOpen() {
        return leftOpen;
    }

    public void setLeftOpen(Boolean leftOpen) {
        this.leftOpen = leftOpen;
        bndVersionRange = null;
    }

    @Column(nullable = true)
    public Boolean isRightOpen() {
        return rightOpen;
    }

    public void setRightOpen(Boolean rightOpen) {
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
                         @AttributeOverride(name = "qualifier", column = @Column(name = "rightQualifier")),})
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
        left = new Version(bndVersionRange.getLow());
        right = new Version(bndVersionRange.getHigh());
        leftOpen = bndVersionRange.includeLow();
        rightOpen = bndVersionRange.includeHigh();
        this.bndVersionRange = bndVersionRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        VersionRange that = (VersionRange) o;

        if (leftOpen != null ? !leftOpen.equals(that.leftOpen) : that.leftOpen != null) return false;
        if (rightOpen != null ? !rightOpen.equals(that.rightOpen) : that.rightOpen != null) return false;
        if (left != null ? !left.equals(that.left) : that.left != null) return false;
        if (right != null ? !right.equals(that.right) : that.right != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = leftOpen != null ? leftOpen.hashCode() : 0;
        result = 31 * result + (rightOpen != null ? rightOpen.hashCode() : 0);
        result = 31 * result + (left != null ? left.hashCode() : 0);
        result = 31 * result + (right != null ? right.hashCode() : 0);
        return result;
    }

    public boolean includes(Version v) {
        return getBndVersionRange().includes(v.getVersion());
    }

    public VersionRange intersect(VersionRange other) {
        return new VersionRange(getBndVersionRange().intersect(other.getBndVersionRange()));
    }

    @Override
    public String toString() {
        return getBndVersionRange().toString();
    }
}
