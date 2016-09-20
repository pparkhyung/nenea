/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package nenea.client.operation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.UnsupportedEncodingException;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

/**
 * Handles a client-side channel.
 */
@Sharable
public class OperationClientHandler extends SimpleChannelInboundHandler<Object> {

	int headerSize;
	String headerBody;
	long dataSize;

	boolean headerRead = false;
	private long offset = 0;
	long startTm;

	String operationCode = "";
	String operationData = "";

	// 파일 관련 멤버
	String fileName, localFileName;
	File recFile;
	FileOutputStream fileOutputStream;

	final static String OP_CODE_SET = "OP_SET"; // agent에게 설정내역을 전송
	final static String OP_CODE_REQUEST = "OP_REQUEST"; // agent에게 정보를 요청
	final static String OP_CODE_SHELL = "OP_SHELL"; // agent에게 shell 실행을 명령
	final static String OP_CODE_JOIN = "OP_JOIN"; // server와 연결 생성 후 가입메시지를 보냄

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		System.out.println("Client " + ctx.channel().remoteAddress() + " channelActive!");
		String msg = "Agent";
		ctx.writeAndFlush(this.initializeProtocol(msg, ctx.channel()));
	}

	// Operation Header 생성
	private ByteBuf initializeProtocol(String msg, Channel ch) throws UnsupportedEncodingException {
		System.out.println("make JOIN Header");
		ByteBuf buf = ch.alloc().heapBuffer(OP_CODE_JOIN.length() + 12 + msg.getBytes("UTF-8").length);

		buf.writeInt(OP_CODE_JOIN.length());
		buf.writeBytes(OP_CODE_JOIN.getBytes());
		buf.writeLong(msg.getBytes("UTF-8").length);
		buf.writeBytes(msg.getBytes("UTF-8"));

		System.out.println("buffer index : " + buf.writerIndex());
		System.out.println("hex : " + ByteBufUtil.hexDump(buf));

		return buf;
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

		ByteBuf buf = (ByteBuf) msg;
		// System.out.println("입력바이트 : " + (ByteBufUtil.hexDump(buf)));

		// 헤더 읽기 처리
		if (!headerRead) {

			startTm = System.currentTimeMillis();

			/*
			 * if (buf.readableBytes() <= 20) {
			 * System.out.println("buf.readableBytes() : " + buf.readableBytes()
			 * + "읽을 바이트가 20도 안됩니다. "); return; }
			 */

			// 1. 헤더 분석
			headerSize = buf.readInt();
			if (headerSize >= 100 || headerSize < 0) {
				System.out.println("fileNameSize : " + headerSize);
				// System.out.println((ByteBufUtil.hexDump(buf)));
				return;
			}

			headerBody = buf.readBytes(headerSize).toString(CharsetUtil.UTF_8);
			dataSize = buf.readLong();
			System.out.println(
					"headerSize : " + headerSize + ", headerBody : " + headerBody + ", dataSize : " + dataSize);

			headerRead = true;

			// 2. 헤더 분류 및 분류에 따른 처리 : operation or file

			if (headerBody.startsWith("OP")) {
				operationCode = headerBody;
			} else if (headerBody.endsWith(".zip") || headerBody.endsWith(".txt") || headerBody.endsWith(".tar")) {
				fileName = headerBody;
				localFileName = "D:\\nene_data\\" + System.currentTimeMillis() + fileName;
				recFile = new File(localFileName);
				fileOutputStream = new FileOutputStream(recFile);
			} else {
				System.err.println("정의한 Header Body가 아닙니다. : " + headerBody);
				return;
			}

		}

		int a = buf.readableBytes();
		int b = (int) Math.min(dataSize - offset, a);
		System.out.println("ReadableBytes : " + a + ", To-Read Bytes : " + b);

		if (headerBody.startsWith("OP")) {

			operationData += buf.readBytes(b).toString(CharsetUtil.UTF_8);
			System.out.println("OP Data 저장 중 : " + operationData);

			offset += b;

			if (offset >= dataSize) {
				offset = 0;
				headerRead = false;
				System.out.println("OP Data : " + operationData);
				System.out.println("OP Data 처리시간 : " + ((System.currentTimeMillis() - startTm) / 1000.0f) + "초");
				operationData = "";
				return;
			}

		} else {

			buf.readBytes(fileOutputStream, b);

			offset += b;
			// System.out.println("offset : " + offset);

			// 파일사이즈만큼 다 읽었으면 초기화
			if (offset >= dataSize) {
				offset = 0;
				headerRead = false;
				if (fileOutputStream != null)
					fileOutputStream.close();
				System.out.println("파일저장완료 : " + localFileName);
				System.out.println("파일저장처리시간 : " + ((System.currentTimeMillis() - startTm) / 1000.0f) + "초");
				return;
			}

		}

	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		System.out.println("channelRead0");
		ByteBuf buf = (ByteBuf) msg;
		System.out.println(buf.readableBytes());
		System.out.println(msg);
		System.out.println(buf.toString(CharsetUtil.UTF_8));
		System.out.println((ByteBufUtil.hexDump(buf)));

		// byte[] msgg = buf.array();
		// System.out.println(buf.readInt());

		int header1 = buf.readInt();
		String header2 = buf.readBytes(header1).toString(CharsetUtil.UTF_8);
		long header3 = buf.readLong();

		System.err.println(header1);
		System.err.println(header2);
		System.err.println(header3);

	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}

}
