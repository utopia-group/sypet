/*
 * Copyright (C) 2017 The SyPet Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package edu.utexas.sypet.synthesis;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.tools.DiagnosticListener;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import edu.utexas.sypet.synthesis.model.Benchmark;
import edu.utexas.sypet.util.TimeUtil;

public class SypetTestUtil {
	public static final boolean DISPLAY_ERROR = false;
	public static final String CLASSNAME = "Source";
	protected static double mTimeCompile = 0;
	protected static double mTimeRun = 0;
	protected static boolean mCompilationSuccess = true;

	public static boolean runTest(Benchmark bench) {
		long start1 = System.nanoTime();
		Class<?> compiledClass = compileClass(bench);
		long end1 = System.nanoTime();
		mTimeCompile = TimeUtil.computeTime(start1, end1);
		if (compiledClass == null) {
			mTimeRun = 0.0;
			return false;
		}
		long start2 = System.nanoTime();
		boolean success = false;
		try{
			Method method = compiledClass.getMethod("test");
			//System.out.println("RESULT=" + (String) method.invoke(null));
			success = (boolean) method.invoke(null);
		} catch (Exception e) {
			if (DISPLAY_ERROR) e.printStackTrace();
			success = false;
		}
		long end2 = System.nanoTime();
		mTimeRun = TimeUtil.computeTime(start2, end2);
		return success;
	}

	public static double getCompilationTime() {
		return mTimeCompile;
	}

	public static double getRunningTime() {
		return mTimeRun;
	}

	public static boolean isCompilationSuccess() {
		return mCompilationSuccess;
	}

	@SuppressWarnings("rawtypes")
	private static Class compileClass(Benchmark bench) {
		String program = genProgram(bench);
		if (DISPLAY_ERROR) System.out.println(program);
		String classpath = genClassPath(bench);
		try{
			@SuppressWarnings("restriction")
//			JavaCompiler javac = new EclipseCompiler();
			JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

			StandardJavaFileManager sjfm = javac.getStandardFileManager(null, null, null);
			SpecialClassLoader cl = new SpecialClassLoader(bench);
			SpecialJavaFileManager fileManager = new SpecialJavaFileManager(sjfm, cl);

			List<String> options = new ArrayList<String>();
			options.add("-cp");
			options.add(classpath);
			List compilationUnits = Arrays.asList(new MemorySource(CLASSNAME, program));
			DiagnosticListener dianosticListener = null;
			Iterable classes = null;
			Writer out = DISPLAY_ERROR ? new PrintWriter(System.err) : null;
			JavaCompiler.CompilationTask compile = javac.getTask(out, fileManager, dianosticListener, options, classes, compilationUnits);
			mCompilationSuccess = compile.call();
			if (mCompilationSuccess) return cl.findClass(CLASSNAME);
		} catch (Exception e){
			if (DISPLAY_ERROR) e.printStackTrace();
		}
		return null;
	}

	private static String genProgram(Benchmark bench) {
		StringBuilder builder = new StringBuilder();
		builder.append("public class ").append(CLASSNAME).append("{\n");
		builder.append("public static ").append(bench.getMethodHeader()).append(" throws Throwable {\n");
		builder.append(bench.getBody());
		builder.append("}\n");
		builder.append(bench.getTestBody());
		builder.append("}\n");
		return builder.toString();
	}

	private static String genClassPath(Benchmark bench) {
		StringBuilder builder = new StringBuilder();
		List<String> libs = bench.getLibs();
		for (String lib : libs) {
			builder.append(lib);
			builder.append(':');
		}
		builder.append('.');
		return builder.toString();
	}

}

class MemorySource extends SimpleJavaFileObject {
	private String src;
	public MemorySource(String name, String src) {
		super(URI.create("file:///" + name + ".java"), Kind.SOURCE);
		this.src = src;
	}
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return src;
	}
	public OutputStream openOutputStream() {
		throw new IllegalStateException();
	}
	public InputStream openInputStream() {
		return new ByteArrayInputStream(src.getBytes());
	}
}

class SpecialJavaFileManager extends ForwardingJavaFileManager {
	private SpecialClassLoader xcl;

	public SpecialJavaFileManager(StandardJavaFileManager sjfm, SpecialClassLoader xcl) {
		super(sjfm);
		this.xcl = xcl;
	}

	public JavaFileObject getJavaFileForOutput(Location location, String name, JavaFileObject.Kind kind, FileObject sibling) throws IOException {
		MemoryByteCode mbc = new MemoryByteCode(name);
		xcl.addClass(name, mbc);
		return mbc;
	}

	public ClassLoader getClassLoader(Location location) {
		return xcl;
	}
}

class MemoryByteCode extends SimpleJavaFileObject {
	private ByteArrayOutputStream baos;

	public MemoryByteCode(String name) {
		super(URI.create("byte:///" + name + ".class"), Kind.CLASS);
	}

	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		throw new IllegalStateException();
	}

	public OutputStream openOutputStream() {
		baos = new ByteArrayOutputStream();
		return baos;
	}

	public InputStream openInputStream() {
		throw new IllegalStateException();
	}

	public byte[] getBytes() {
		return baos.toByteArray();
	}
}

class SpecialClassLoader extends ClassLoader {
	protected Map<String, MemoryByteCode> map = new HashMap<String, MemoryByteCode>();
	protected Benchmark mBench;
	protected URLClassLoader cl = null;

	public SpecialClassLoader(Benchmark bench) {
		mBench = bench;
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException {
		MemoryByteCode mbc = map.get(name);
		if (mbc == null){
			URL[] urls = getUrls(mBench);
			if (cl == null) {
				cl = new URLClassLoader(urls, Thread.currentThread().getContextClassLoader());
			}
			return cl.loadClass(name);
		} else {
			return defineClass(name, mbc.getBytes(), 0, mbc.getBytes().length);
		}
	}

	public void addClass(String name, MemoryByteCode mbc) {
		map.put(name, mbc);
	}

	protected URL[] getUrls(Benchmark bench) {
		ArrayList<String> libs = new ArrayList<String>(bench.getLibs());
		URL[] urls = new URL[libs.size()];
		try {
			for (int i = 0; i < libs.size(); ++i) {
				urls[i] = new File(libs.get(i)).toURI().toURL();
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return urls;
	}
}
